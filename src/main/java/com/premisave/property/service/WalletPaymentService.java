package com.premisave.property.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.premisave.property.client.AuthServiceClient;
import com.premisave.property.client.WalletServiceClient;
import com.premisave.property.dto.response.UserDto;
import com.premisave.property.dto.wallet.WalletApiResponse;
import com.premisave.property.dto.wallet.WalletTransactionResult;
import com.premisave.property.dto.wallet.WalletTransferRequest;
import com.premisave.property.entity.Owner;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.Tenant;
import com.premisave.property.entity.WalletTransfer;
import com.premisave.property.enums.PaymentMethod;
import com.premisave.property.enums.WalletTransferPurpose;
import com.premisave.property.enums.WalletTransferStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.ServiceOfflineException;
import com.premisave.property.exception.WalletServiceException;
import com.premisave.property.health.ExternalService;
import com.premisave.property.health.FeignFailures;
import com.premisave.property.health.ServiceHealthMonitor;
import com.premisave.property.repository.OwnerRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.TenantRepository;
import com.premisave.property.repository.WalletTransferRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Moves money for property-service payments: the tenant's wallet is debited and
 * the property owner's wallet is credited in a single wallet-service
 * /internal/transfer call.
 *
 * Callers (rent / utility payment services) follow this order:
 *   1. validate everything that can be rejected locally
 *   2. {@link #collect} — moves the money (idempotent per reference)
 *   3. book the payment locally
 *   4. {@link #markBooked} (or {@link #bookingFailed} if step 3 threw)
 *
 * Every attempt is recorded in the wallet_transfers collection, so a retry with
 * the same reference can never debit the tenant twice, and a payment that was
 * debited but not booked can be completed by simply retrying.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletPaymentService {

    private static final String INITIATED_BY = "PROPERTY_SERVICE";
    private static final String REFERENCE_PREFIX = "PRP-";
    private static final Pattern CLIENT_REFERENCE = Pattern.compile("^[A-Za-z0-9._:-]{1,40}$");
    private static final long IN_FLIGHT_SECONDS = 30;
    private static final String IN_PROGRESS_MESSAGE =
            "This payment is already being processed. Please wait a few seconds and check your "
                    + "payment history before retrying.";

    private final WalletServiceClient walletServiceClient;
    private final AuthServiceClient authServiceClient;
    private final WalletTransferRepository walletTransferRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final OwnerRepository ownerRepository;
    private final ObjectMapper objectMapper;
    private final PaymentNotificationService paymentNotificationService;
    private final ServiceHealthMonitor healthMonitor;

    @Value("${app.api-key}")
    private String internalApiKey;

    /**
     * @param reference   wallet reference from {@link #resolveReference}
     * @param propertyId  the property whose owner receives the money
     * @param targetId    leaseId / rentalUnitId / billId being paid
     */
    public record CollectionCommand(String reference, String tenantId, String propertyId,
                                    WalletTransferPurpose purpose, String targetId,
                                    BigDecimal amount, String description) {
    }

    // ------------------------------------------------------------------
    // Request helpers
    // ------------------------------------------------------------------

    /**
     * Payments are made from the tenant's Premisave wallet. Funding the wallet
     * with M-Pesa / Stripe / PayPal happens in wallet-service, before this.
     */
    public void assertWalletPaymentMethod(PaymentMethod method) {
        if (method != null && method != PaymentMethod.WALLET) {
            throw new BadRequestException(
                    "Payments are made from your Premisave wallet. Top up your wallet with " + method
                            + " first, then pay using paymentMethod WALLET.");
        }
    }

    /**
     * Builds the reference sent to wallet-service. A client-supplied reference
     * is the idempotency key (retrying with it never double-charges); without
     * one, a random reference is generated and retries are not protected.
     * Namespaced with the tenant id so two tenants can never collide.
     */
    public String resolveReference(String tenantId, String clientReference) {
        if (clientReference == null || clientReference.isBlank()) {
            return REFERENCE_PREFIX + tenantId + "-" + UUID.randomUUID();
        }
        String trimmed = clientReference.trim();
        if (!CLIENT_REFERENCE.matcher(trimmed).matches()) {
            throw new BadRequestException(
                    "reference must be 1-40 characters: letters, digits, '.', '_', ':' or '-'");
        }
        return REFERENCE_PREFIX + tenantId + "-" + trimmed;
    }

    /** The part of a wallet reference a client passes back as {@code reference} to retry. */
    public String clientFacingReference(String reference) {
        int idx = reference.indexOf('-', REFERENCE_PREFIX.length());
        return idx >= 0 ? reference.substring(idx + 1) : reference;
    }

    // ------------------------------------------------------------------
    // Money movement
    // ------------------------------------------------------------------

    /**
     * Ensures the money for this command has been moved, exactly once.
     * Returns a transfer in status COMPLETED (or BOOKED if it was already
     * booked). Throws BadRequestException if wallet-service rejected the
     * transfer, WalletServiceException if the outcome could not be confirmed.
     */
    public WalletTransfer collect(CollectionCommand cmd) {
        validateAmount(cmd.amount());

        Optional<WalletTransfer> existing = walletTransferRepository.findByReference(cmd.reference());

        if (existing.isPresent()) {
            WalletTransfer transfer = existing.get();
            assertSameCollection(transfer, cmd);

            return switch (transfer.getStatus()) {
                case FAILED -> throw new ConflictException(
                        "This payment reference was already used by an attempt that failed ("
                                + transfer.getFailureReason() + "). Retry with a new reference.");
                case BOOKED -> transfer;
                case COMPLETED -> claim(transfer);              // debited earlier; just finish booking
                case INITIATED -> execute(claim(transfer));     // outcome unknown; wallet dedupes by reference
            };
        }

        return execute(createTransfer(cmd));
    }

    /** Marks the transfer as fully booked. Never throws — the payment is already recorded. */
    public void markBooked(String reference, String bookedRecordId) {
        try {
            walletTransferRepository.findByReference(reference).ifPresent(transfer -> {
                transfer.setStatus(WalletTransferStatus.BOOKED);
                transfer.setBookedRecordId(bookedRecordId);
                transfer.setInFlightUntil(null);
                walletTransferRepository.save(transfer);
            });
        } catch (RuntimeException e) {
            log.error("Payment was booked as {} but wallet transfer {} could not be marked BOOKED: {}",
                    bookedRecordId, reference, e.getMessage());
        }
    }

    /**
     * Call when local booking threw after {@link #collect} succeeded. The money
     * has moved, so this releases the in-flight lock (so a retry can finish the
     * booking immediately) and returns the exception to throw to the client.
     */
    public WalletServiceException bookingFailed(WalletTransfer transfer, RuntimeException cause) {
        log.error("MONEY MOVED BUT PAYMENT NOT BOOKED — wallet transfer {} (walletTransactionId={}, tenantId={}, "
                        + "amount={}, purpose={}, target={}). A retry with the same reference completes it.",
                transfer.getReference(), transfer.getWalletTransactionId(), transfer.getTenantId(),
                transfer.getAmount(), transfer.getPurpose(), transfer.getTargetId(), cause);
        try {
            walletTransferRepository.findByReference(transfer.getReference()).ifPresent(t -> {
                t.setInFlightUntil(null);
                walletTransferRepository.save(t);
            });
        } catch (RuntimeException e) {
            log.error("Could not release in-flight lock for wallet transfer {}: {}",
                    transfer.getReference(), e.getMessage());
        }
        return new WalletServiceException(
                "Your wallet was debited but we couldn't finish recording the payment. Retry the same request "
                        + "with reference \"" + clientFacingReference(transfer.getReference())
                        + "\" to complete it — you will not be charged twice.", "PAYMENT_NOT_RECORDED", cause);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private WalletTransfer createTransfer(CollectionCommand cmd) {
        Tenant tenant = tenantRepository.findById(cmd.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        if (tenant.getUserId() == null || tenant.getUserId().isBlank()) {
            throw new BadRequestException(
                    "Your tenant profile isn't linked to a user account, so a wallet payment can't be made");
        }

        Property property = propertyRepository.findById(cmd.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        Owner owner = ownerRepository.findById(property.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Property owner not found"));

        if (tenant.getUserId().equals(owner.getUserId())) {
            throw new BadRequestException("You can't pay for a property you own");
        }

        WalletTransfer transfer = new WalletTransfer();
        transfer.setReference(cmd.reference());
        transfer.setPurpose(cmd.purpose());
        transfer.setTargetId(cmd.targetId());
        transfer.setTenantId(cmd.tenantId());
        transfer.setOwnerId(owner.getId());
        transfer.setPropertyId(cmd.propertyId());
        transfer.setSenderUserId(tenant.getUserId());
        transfer.setRecipientAccount(resolveOwnerAccount(owner));
        transfer.setAmount(cmd.amount());
        transfer.setDescription(cmd.description());
        transfer.setStatus(WalletTransferStatus.INITIATED);
        transfer.setInFlightUntil(LocalDateTime.now().plusSeconds(IN_FLIGHT_SECONDS));

        try {
            return walletTransferRepository.save(transfer);
        } catch (DuplicateKeyException e) {
            // Another request created this reference a moment ago (unique index).
            throw new ConflictException(IN_PROGRESS_MESSAGE);
        }
    }

    /**
     * The owner's wallet account number is their auth-service email. It is
     * resolved from auth-service via the JWT-derived Owner.userId — never from
     * Owner.email, which an owner can type freely and would let them route
     * rent to someone else's wallet.
     */
    private String resolveOwnerAccount(Owner owner) {
        if (owner.getUserId() == null || owner.getUserId().isBlank()) {
            throw new BadRequestException("The property owner's profile isn't linked to a user account");
        }
        UserDto ownerUser;
        try {
            ownerUser = authServiceClient.getUserById(owner.getUserId(), internalApiKey);
        } catch (FeignException.NotFound e) {
            throw new BadRequestException("The property owner's account could not be found");
        } catch (FeignException e) {
            if (FeignFailures.looksOffline(e)) {
                healthMonitor.markDown(ExternalService.AUTH);
                throw new ServiceOfflineException(ExternalService.AUTH, "process payments", "Nothing has been charged.");
            }
            // auth-service is up but failed this lookup — e.g. it rejected OUR api key (401/403) or errored.
            // The payment can't proceed and nothing has moved.
            log.error("auth-service lookup for owner user {} failed (HTTP {}): {}",
                    owner.getUserId(), e.status(), e.getMessage());
            throw new WalletServiceException(
                    "We can't process payments right now. No money was taken from your wallet. "
                            + "Please try again later.", "PAYMENT_SERVICE_ERROR");
        }
        if (ownerUser == null || ownerUser.getEmail() == null || ownerUser.getEmail().isBlank()) {
            throw new BadRequestException("The property owner has no wallet account on file");
        }
        return ownerUser.getEmail();
    }

    /** Takes the in-flight lock; the @Version check makes concurrent claims mutually exclusive. */
    private WalletTransfer claim(WalletTransfer transfer) {
        if (transfer.getInFlightUntil() != null && transfer.getInFlightUntil().isAfter(LocalDateTime.now())) {
            throw new ConflictException(IN_PROGRESS_MESSAGE);
        }
        transfer.setInFlightUntil(LocalDateTime.now().plusSeconds(IN_FLIGHT_SECONDS));
        try {
            return walletTransferRepository.save(transfer);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictException(IN_PROGRESS_MESSAGE);
        }
    }

    private WalletTransfer execute(WalletTransfer transfer) {
        WalletApiResponse<WalletTransactionResult> response;
        try {
            response = walletServiceClient.transfer(WalletTransferRequest.builder()
                    .senderUserId(transfer.getSenderUserId())
                    .recipientAccountNumber(transfer.getRecipientAccount())
                    .amount(transfer.getAmount())
                    .description(transfer.getDescription())
                    .reference(transfer.getReference())
                    .initiatedBy(INITIATED_BY)
                    .build());
        } catch (FeignException e) {
            throw handleFeignFailure(transfer, e);
        } catch (RuntimeException e) {
            log.error("Unexpected error calling wallet-service for transfer {}", transfer.getReference(), e);
            release(transfer, WalletTransferStatus.INITIATED, null);
            throw outcomeUnknown(transfer, e);
        }

        WalletTransactionResult result = response != null ? response.getData() : null;
        boolean succeeded = response != null && response.isSuccess() && result != null && result.isSuccess();

        if (!succeeded) {
            String reason = firstNonBlank(
                    result != null ? result.getMessage() : null,
                    response != null ? response.getMessage() : null,
                    "The wallet service did not complete the transfer");
            release(transfer, WalletTransferStatus.FAILED, reason);
            notifyRejected(transfer, reason);
            throw new BadRequestException(reason);
        }

        // Keep the in-flight lock: the caller is about to book this payment.
        transfer.setStatus(WalletTransferStatus.COMPLETED);
        transfer.setWalletTransactionId(result.getTransactionId());
        transfer.setFailureReason(null);
        return walletTransferRepository.save(transfer);
    }

    private RuntimeException handleFeignFailure(WalletTransfer transfer, FeignException e) {
        int status = e.status();

        // The request never reached wallet-service (connection refused, unknown host, connect timeout):
        // nothing was sent, so nothing moved. Keep the record INITIATED so a retry with the same
        // reference simply sends it again.
        if (FeignFailures.neverReached(e)) {
            log.warn("wallet-service unreachable for transfer {}: {}", transfer.getReference(), e.getMessage());
            healthMonitor.markDown(ExternalService.WALLET);
            release(transfer, WalletTransferStatus.INITIATED, null);
            return new ServiceOfflineException(ExternalService.WALLET, "process payments", "Nothing has been charged.");
        }

        // wallet-service auth problem — nothing moved; this is our misconfiguration.
        if (status == 401 || status == 403) {
            log.error("wallet-service rejected our service credentials (HTTP {}). "
                    + "Check INTERNAL_API_KEY matches on both services.", status);
            release(transfer, WalletTransferStatus.FAILED, "Payment service authentication failed");
            return new WalletServiceException(
                    "We can't process payments right now. No money was taken from your wallet. "
                            + "Please try again later.", "PAYMENT_SERVICE_ERROR");
        }

        // Definitive rejection (insufficient funds, unknown recipient, validation...) — nothing moved.
        if (status >= 400 && status < 500 && status != 408 && status != 429) {
            String reason = extractMessage(e);
            release(transfer, WalletTransferStatus.FAILED, reason);
            notifyRejected(transfer, reason);
            return new BadRequestException(reason);
        }

        // 5xx, timeouts, connection errors, undecodable 200s: we can't know whether money moved.
        log.error("wallet-service transfer {} outcome unknown (HTTP {}): {}",
                transfer.getReference(), status, e.getMessage());
        release(transfer, WalletTransferStatus.INITIATED, null);
        return outcomeUnknown(transfer, e);
    }

    /** Tells the tenant (email + SMS, async) that wallet-service rejected the payment and nothing moved. */
    private void notifyRejected(WalletTransfer transfer, String reason) {
        paymentNotificationService.notifyPaymentFailed(transfer.getTenantId(), transfer.getDescription(),
                transfer.getAmount(), reason, clientFacingReference(transfer.getReference()));
    }

    private WalletServiceException outcomeUnknown(WalletTransfer transfer, Throwable cause) {
        return new WalletServiceException(
                "We couldn't confirm your payment with the Premisave wallet. If your wallet was debited, retry "
                        + "the same request with reference \"" + clientFacingReference(transfer.getReference())
                        + "\" to complete it — you will not be charged twice.", "PAYMENT_UNCONFIRMED", cause);
    }

    /** Sets a status and releases the in-flight lock. */
    private void release(WalletTransfer transfer, WalletTransferStatus status, String failureReason) {
        try {
            transfer.setStatus(status);
            transfer.setFailureReason(failureReason);
            transfer.setInFlightUntil(null);
            walletTransferRepository.save(transfer);
        } catch (RuntimeException e) {
            log.error("Could not update wallet transfer {} to {}: {}", transfer.getReference(), status, e.getMessage());
        }
    }

    private String extractMessage(FeignException e) {
        try {
            String body = e.contentUTF8();
            if (body != null && !body.isBlank()) {
                JsonNode node = objectMapper.readTree(body);
                for (String field : List.of("message", "error")) {
                    JsonNode value = node.get(field);
                    if (value != null && value.isTextual() && !value.asText().isBlank()) {
                        return value.asText();
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through to the generic message
        }
        return "The wallet service rejected the payment";
    }

    private void assertSameCollection(WalletTransfer transfer, CollectionCommand cmd) {
        boolean same = transfer.getPurpose() == cmd.purpose()
                && transfer.getTenantId().equals(cmd.tenantId())
                && transfer.getTargetId().equals(cmd.targetId())
                && transfer.getAmount().compareTo(cmd.amount()) == 0;
        if (!same) {
            throw new ConflictException("This payment reference was already used for a different payment");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException("Payment amount can have at most 2 decimal places");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}