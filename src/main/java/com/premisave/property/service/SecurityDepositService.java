package com.premisave.property.service;

import com.premisave.property.dto.request.RefundDepositRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.DepositRefundEntryResponse;
import com.premisave.property.dto.response.LeaseSummaryResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RefundCheckResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.SecurityDepositResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.entity.DepositRefundEntry;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.SecurityDeposit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.enums.DepositStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.SecurityDepositRepository;
import com.premisave.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityDepositService {

    private final SecurityDepositRepository depositRepository;
    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Transactional
    public SecurityDepositResponse holdDeposit(SecurityDepositRequest request) {
        boolean hasLease = request.getLeaseId() != null && !request.getLeaseId().isBlank();
        boolean hasUnit = request.getRentalUnitId() != null && !request.getRentalUnitId().isBlank();

        if (hasLease == hasUnit) {
            throw new BadRequestException("Provide exactly one of leaseId or rentalUnitId");
        }

        SecurityDeposit saved = hasLease ? holdLeaseDeposit(request) : holdUnitDeposit(request);
        taskExecutor.execute(() -> notifyDepositHeld(saved));
        return toResponse(saved);
    }

    private SecurityDeposit holdLeaseDeposit(SecurityDepositRequest request) {
        depositRepository.findByLeaseId(request.getLeaseId()).ifPresent(existing -> {
            throw new ConflictException("A security deposit already exists for this lease");
        });

        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        SecurityDeposit deposit = new SecurityDeposit();
        deposit.setLeaseId(request.getLeaseId());
        deposit.setTenantId(lease.getTenantId());
        deposit.setAmount(request.getAmount());
        deposit.setRefundedAmount(BigDecimal.ZERO);
        deposit.setStatus(DepositStatus.HELD);

        return depositRepository.save(deposit);
    }

    private SecurityDeposit holdUnitDeposit(SecurityDepositRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new BadRequestException(
                    "tenantId is required when holding a deposit against a rentalUnitId");
        }

        // Only an unresolved (not-yet-fully-refunded) deposit blocks a new
        // hold. A fully REFUNDED deposit is a closed-out record from a past
        // tenancy period and should never prevent the same tenant from
        // returning to the same unit later and starting a new one.
        findActiveUnitDeposit(request.getRentalUnitId(), request.getTenantId())
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "This tenant already has an active security deposit on this unit");
                });

        SecurityDeposit deposit = new SecurityDeposit();
        deposit.setRentalUnitId(request.getRentalUnitId());
        deposit.setTenantId(request.getTenantId());
        deposit.setAmount(request.getAmount());
        deposit.setRefundedAmount(BigDecimal.ZERO);
        deposit.setStatus(DepositStatus.HELD);

        return depositRepository.save(deposit);
    }

    /**
     * Refunds part or all of a deposit. Can be called multiple times — each
     * call adds one entry to the refund history, and the running total of
     * every refund issued so far can never exceed the original deposit
     * amount. A reason is required whenever the refund is partial (i.e.
     * doesn't bring the balance to zero); it's optional on a final refund
     * that fully closes the deposit out. Fires a best-effort email + SMS to
     * the tenant once the refund is recorded.
     */
    @Transactional
    public SecurityDepositResponse refundDeposit(RefundDepositRequest request) {
        boolean hasLease = request.getLeaseId() != null && !request.getLeaseId().isBlank();
        boolean hasUnit = request.getRentalUnitId() != null && !request.getRentalUnitId().isBlank();

        if (hasLease == hasUnit) {
            throw new BadRequestException("Provide exactly one of leaseId or rentalUnitId");
        }

        SecurityDeposit deposit = hasLease
                ? depositRepository.findByLeaseId(request.getLeaseId())
                        .orElseThrow(() -> new ResourceNotFoundException("No security deposit found for this lease"))
                : findActiveUnitDeposit(request.getRentalUnitId(), requireTenantId(request))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No active security deposit found for this tenant on this unit"));

        if (deposit.getStatus() == DepositStatus.REFUNDED) {
            throw new ConflictException("This deposit has already been fully refunded");
        }

        BigDecimal requestedAmount = request.getAmount();
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be greater than zero");
        }

        BigDecimal alreadyRefunded = deposit.getRefundedAmount() != null
                ? deposit.getRefundedAmount()
                : BigDecimal.ZERO;
        BigDecimal remaining = deposit.getAmount().subtract(alreadyRefunded);

        if (requestedAmount.compareTo(remaining) > 0) {
            throw new BadRequestException(
                    "Refund amount (" + requestedAmount + ") exceeds the remaining refundable balance ("
                            + remaining + "). Total refunds cannot exceed the deposit held.");
        }

        boolean isFinalRefund = requestedAmount.compareTo(remaining) == 0;

        if (!isFinalRefund && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("A reason is required when issuing a partial refund");
        }

        DepositRefundEntry entry = new DepositRefundEntry();
        entry.setAmount(requestedAmount);
        entry.setReason(request.getReason());
        entry.setRefundedAt(LocalDateTime.now());

        if (deposit.getRefundHistory() == null) {
            deposit.setRefundHistory(new ArrayList<>());
        }
        deposit.getRefundHistory().add(entry);

        BigDecimal newRefundedTotal = alreadyRefunded.add(requestedAmount);
        deposit.setRefundedAmount(newRefundedTotal);
        deposit.setRefundedAt(entry.getRefundedAt());
        deposit.setStatus(isFinalRefund ? DepositStatus.REFUNDED : DepositStatus.PARTIALLY_REFUNDED);

        SecurityDeposit saved = depositRepository.save(deposit);

        BigDecimal newRemaining = saved.getAmount().subtract(newRefundedTotal);
        taskExecutor.execute(() ->
                notifyRefund(saved, requestedAmount, entry.getReason(), isFinalRefund, newRemaining));

        return toResponse(saved);
    }

    /**
     * Read-only preview for a given candidate refund amount — tells the
     * caller whether it would be a full or partial refund (and therefore
     * whether the frontend should show the reason field) without actually
     * recording anything. Mirrors the validation in refundDeposit() exactly,
     * so a check here always matches what the real refund call will do.
     */
    public RefundCheckResponse checkRefund(String leaseId, String rentalUnitId, String tenantId,
                                            BigDecimal amount) {
        boolean hasLease = leaseId != null && !leaseId.isBlank();
        boolean hasUnit = rentalUnitId != null && !rentalUnitId.isBlank();

        if (hasLease == hasUnit) {
            throw new BadRequestException("Provide exactly one of leaseId or rentalUnitId");
        }

        SecurityDeposit deposit = hasLease
                ? depositRepository.findByLeaseId(leaseId)
                        .orElseThrow(() -> new ResourceNotFoundException("No security deposit found for this lease"))
                : findActiveUnitDeposit(rentalUnitId, requireTenantId(tenantId))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No active security deposit found for this tenant on this unit"));

        BigDecimal alreadyRefunded = deposit.getRefundedAmount() != null
                ? deposit.getRefundedAmount()
                : BigDecimal.ZERO;
        BigDecimal remaining = deposit.getAmount().subtract(alreadyRefunded);

        RefundCheckResponse response = new RefundCheckResponse();
        response.setDepositAmount(deposit.getAmount());
        response.setAlreadyRefunded(alreadyRefunded);
        response.setRemainingRefundable(remaining);
        response.setRequestedAmount(amount);

        if (deposit.getStatus() == DepositStatus.REFUNDED) {
            response.setFullRefund(false);
            response.setPartialRefund(false);
            response.setExceedsRemainingBalance(true);
            response.setReasonRequired(false);
            response.setMessage("This deposit has already been fully refunded — nothing left to refund.");
            return response;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            response.setFullRefund(false);
            response.setPartialRefund(false);
            response.setExceedsRemainingBalance(false);
            response.setReasonRequired(false);
            response.setMessage("Enter an amount greater than zero.");
            return response;
        }

        int comparison = amount.compareTo(remaining);

        if (comparison > 0) {
            response.setFullRefund(false);
            response.setPartialRefund(false);
            response.setExceedsRemainingBalance(true);
            response.setReasonRequired(false);
            response.setMessage("This amount exceeds the remaining refundable balance of " + remaining + ".");
        } else if (comparison == 0) {
            response.setFullRefund(true);
            response.setPartialRefund(false);
            response.setExceedsRemainingBalance(false);
            response.setReasonRequired(false);
            response.setMessage("This will fully close out the deposit.");
        } else {
            response.setFullRefund(false);
            response.setPartialRefund(true);
            response.setExceedsRemainingBalance(false);
            response.setReasonRequired(true);
            response.setMessage("This is a partial refund — a reason is required.");
        }

        return response;
    }

    public SecurityDepositResponse getDepositByLease(String leaseId) {
        return depositRepository.findByLeaseId(leaseId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No security deposit found for this lease"));
    }

    // Returns the most recent deposit for this unit+tenant pair regardless
    // of status — so a tenant/owner can still view a past tenancy's
    // deposit (and its refund history) even after it's been fully
    // refunded and closed out, rather than getting a 404.
    public SecurityDepositResponse getDepositByUnit(String rentalUnitId, String tenantId) {
        return findLatestUnitDeposit(rentalUnitId, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No security deposit found for this tenant on this unit"));
    }

    // Whether this tenant currently has an unresolved deposit on this unit —
    // i.e. one from their CURRENT tenancy, not a fully REFUNDED one left
    // over from a past stay. Used by UnitRentPaymentService to decide
    // whether a new deposit charge is still owed.
    public boolean hasActiveDeposit(String rentalUnitId, String tenantId) {
        return findActiveUnitDeposit(rentalUnitId, tenantId).isPresent();
    }

    // ------------------------------------------------------------------
    // Notifications — best-effort, mirrors EmailService/SmsService's own
    // contract: never allowed to throw or block the deposit/refund that
    // already committed successfully. Skipped silently if the tenant
    // can't be resolved; each service already skips on its own if the
    // tenant has no email/phone on file.
    //
    // Dispatched via taskExecutor.execute(...) at the call site rather than
    // via @Async: an @Async annotation on a method called from elsewhere in
    // this same class would be silently ignored (Spring's proxy never sees
    // the call), so submitting to the shared executor bean directly is the
    // correct way to get this off the request thread without introducing a
    // separate bean just to host @Async methods.
    // ------------------------------------------------------------------

    private void notifyDepositHeld(SecurityDeposit deposit) {
        Tenant tenant = tenantRepository.findById(deposit.getTenantId()).orElse(null);
        if (tenant == null) {
            log.warn("Skipping deposit-held notification — tenant {} not found", deposit.getTenantId());
            return;
        }

        String location = resolveLocationSummary(deposit);

        String subject = "Security Deposit Received";
        String content = "We've recorded and held your security deposit of KES " + deposit.getAmount()
                + " for " + location + ". This will be refunded (in full or in part) when your tenancy ends, "
                + "subject to the condition of the property.";
        String smsMessage = "Premisave: Your security deposit of KES " + deposit.getAmount()
                + " for " + location + " has been recorded and held.";

        emailService.sendNoticeEmail(tenant.getEmail(), tenant.getFullName(), subject,
                "SECURITY_DEPOSIT_HELD", content);
        smsService.sendNoticeSms(tenant.getPhoneNumber(), smsMessage);
    }

    private void notifyRefund(SecurityDeposit deposit, BigDecimal refundedNow, String reason,
                               boolean isFinalRefund, BigDecimal remaining) {
        Tenant tenant = tenantRepository.findById(deposit.getTenantId()).orElse(null);
        if (tenant == null) {
            log.warn("Skipping refund notification — tenant {} not found", deposit.getTenantId());
            return;
        }

        String location = resolveLocationSummary(deposit);
        String subject = isFinalRefund ? "Security Deposit Fully Refunded" : "Security Deposit Partially Refunded";

        StringBuilder content = new StringBuilder("A refund of KES ").append(refundedNow)
                .append(" has been issued against your security deposit for ").append(location).append(".");
        if (reason != null && !reason.isBlank()) {
            content.append(" Reason: ").append(reason).append(".");
        }
        if (isFinalRefund) {
            content.append(" Your deposit has now been fully refunded.");
        } else {
            content.append(" Remaining balance still held: KES ").append(remaining).append(".");
        }

        StringBuilder smsMessage = new StringBuilder("Premisave: KES ").append(refundedNow)
                .append(isFinalRefund ? " refunded — deposit fully settled (" : " refunded from your deposit (")
                .append(location).append(").");
        if (!isFinalRefund) {
            smsMessage.append(" Remaining: KES ").append(remaining).append(".");
        }

        emailService.sendNoticeEmail(tenant.getEmail(), tenant.getFullName(), subject,
                "SECURITY_DEPOSIT_REFUND", content.toString());
        smsService.sendNoticeSms(tenant.getPhoneNumber(), smsMessage.toString());
    }

    /**
     * Resolves "which property/unit is this deposit about" the same way
     * enrichWithSummaries() does for the API response — lease-backed
     * deposits pull property (and unit, if any) from the Lease; unit-backed
     * deposits pull property from the RentalUnit directly. Kept separate
     * from enrichWithSummaries since that method populates a response DTO,
     * while this one only needs a short display string for notifications.
     */
    private String resolveLocationSummary(SecurityDeposit deposit) {
        String propertyId = null;
        String rentalUnitId = deposit.getRentalUnitId();

        if (deposit.getLeaseId() != null) {
            Lease lease = leaseRepository.findById(deposit.getLeaseId()).orElse(null);
            if (lease != null) {
                propertyId = lease.getPropertyId();
                if (rentalUnitId == null) {
                    rentalUnitId = lease.getRentalUnitId(); // null for whole-property leases
                }
            }
        }

        RentalUnit unit = rentalUnitId != null ? rentalUnitRepository.findById(rentalUnitId).orElse(null) : null;
        if (propertyId == null && unit != null) {
            propertyId = unit.getPropertyId();
        }

        Property property = propertyId != null ? propertyRepository.findById(propertyId).orElse(null) : null;

        return locationSummary(unit, property);
    }

    /**
     * Human-readable "which property/unit is this about" fragment, used in
     * both notification emails and SMS so a tenant with more than one
     * deposit can tell them apart at a glance. Falls back gracefully if
     * property or unit lookups came back empty — never lets a missing
     * lookup break the notification itself.
     */
    private String locationSummary(RentalUnit unit, Property property) {
        String propertyName = (property != null && property.getTitle() != null && !property.getTitle().isBlank())
                ? property.getTitle() : "your property";

        if (unit != null && unit.getUnitNumber() != null && !unit.getUnitNumber().isBlank()) {
            return propertyName + ", Unit " + unit.getUnitNumber();
        }
        return propertyName;
    }

    // ------------------------------------------------------------------
    // Direct-unit deposit lookups — a tenant can have more than one
    // SecurityDeposit document over time for the same unit (one per
    // tenancy period), so these centralize how "the deposit that matters
    // right now" vs. "the most recent one overall" gets picked.
    // ------------------------------------------------------------------

    private Optional<SecurityDeposit> findActiveUnitDeposit(String rentalUnitId, String tenantId) {
        return depositRepository.findByRentalUnitIdAndTenantIdOrderByCreatedAtDesc(rentalUnitId, tenantId)
                .stream()
                .filter(d -> d.getStatus() != DepositStatus.REFUNDED)
                .findFirst();
    }

    private Optional<SecurityDeposit> findLatestUnitDeposit(String rentalUnitId, String tenantId) {
        return depositRepository.findByRentalUnitIdAndTenantIdOrderByCreatedAtDesc(rentalUnitId, tenantId)
                .stream()
                .findFirst();
    }

    private String requireTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("tenantId is required for this rentalUnitId operation");
        }
        return tenantId;
    }

    private String requireTenantId(RefundDepositRequest request) {
        return requireTenantId(request.getTenantId());
    }

    // ------------------------------------------------------------------
    // Response building — including enrichment with tenant/lease/property/
    // unit summaries so the frontend can render a full picture in one call.
    // ------------------------------------------------------------------

    private SecurityDepositResponse toResponse(SecurityDeposit deposit) {
        SecurityDepositResponse response = new SecurityDepositResponse();
        response.setId(deposit.getId());
        response.setLeaseId(deposit.getLeaseId());
        response.setRentalUnitId(deposit.getRentalUnitId());
        response.setTenantId(deposit.getTenantId());
        response.setAmount(deposit.getAmount());
        response.setRefundedAmount(deposit.getRefundedAmount());

        BigDecimal refunded = deposit.getRefundedAmount() != null ? deposit.getRefundedAmount() : BigDecimal.ZERO;
        response.setRemainingRefundable(deposit.getAmount().subtract(refunded));

        response.setStatus(deposit.getStatus());
        response.setRefundedAt(deposit.getRefundedAt());
        response.setRefundHistory(toRefundHistoryResponse(deposit.getRefundHistory()));

        enrichWithSummaries(response, deposit);

        return response;
    }

    private void enrichWithSummaries(SecurityDepositResponse response, SecurityDeposit deposit) {
        if (deposit.getTenantId() != null) {
            tenantRepository.findById(deposit.getTenantId())
                    .ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }

        String propertyId = null;
        String rentalUnitId = deposit.getRentalUnitId();

        if (deposit.getLeaseId() != null) {
            Lease lease = leaseRepository.findById(deposit.getLeaseId()).orElse(null);
            if (lease != null) {
                response.setLease(toLeaseSummary(lease));
                propertyId = lease.getPropertyId();
                if (rentalUnitId == null) {
                    rentalUnitId = lease.getRentalUnitId(); // null for whole-property leases
                }
            }
        }

        if (rentalUnitId != null) {
            RentalUnit unit = rentalUnitRepository.findById(rentalUnitId).orElse(null);
            if (unit != null) {
                response.setUnit(toRentalUnitSummary(unit));
                if (propertyId == null) {
                    propertyId = unit.getPropertyId();
                }
            }
        }

        if (propertyId != null) {
            propertyRepository.findById(propertyId)
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }
    }

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        TenantSummaryResponse summary = new TenantSummaryResponse();
        summary.setId(tenant.getId());
        summary.setFullName(tenant.getFullName());
        summary.setPhoneNumber(tenant.getPhoneNumber());
        summary.setEmail(tenant.getEmail());
        return summary;
    }

    private LeaseSummaryResponse toLeaseSummary(Lease lease) {
        LeaseSummaryResponse summary = new LeaseSummaryResponse();
        summary.setId(lease.getId());
        summary.setLeaseType(lease.getLeaseType());
        summary.setStartDate(lease.getStartDate());
        summary.setEndDate(lease.getEndDate());
        summary.setMonthlyRent(lease.getMonthlyRent());
        summary.setStatus(lease.getStatus());
        return summary;
    }

    private PropertySummaryResponse toPropertySummary(Property property) {
        PropertySummaryResponse summary = new PropertySummaryResponse();
        summary.setId(property.getId());
        summary.setTitle(property.getTitle());
        summary.setPropertyType(property.getPropertyType());
        summary.setAddress(toAddressResponse(property.getAddress()));
        summary.setRegistrationNumber(property.getRegistrationNumber());
        return summary;
    }

    private RentalUnitSummaryResponse toRentalUnitSummary(RentalUnit unit) {
        RentalUnitSummaryResponse summary = new RentalUnitSummaryResponse();
        summary.setId(unit.getId());
        summary.setUnitNumber(unit.getUnitNumber());
        summary.setFloor(unit.getFloor());
        summary.setRentAmount(unit.getRentAmount());
        summary.setStatus(unit.getStatus());
        return summary;
    }

    private com.premisave.property.dto.response.AddressResponse toAddressResponse(
            com.premisave.property.entity.Address address) {
        if (address == null) {
            return null;
        }
        com.premisave.property.dto.response.AddressResponse response =
                new com.premisave.property.dto.response.AddressResponse();
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setLandmark(address.getLandmark());
        return response;
    }

    private List<DepositRefundEntryResponse> toRefundHistoryResponse(List<DepositRefundEntry> history) {
        if (history == null) {
            return List.of();
        }
        return history.stream()
                .map(entry -> {
                    DepositRefundEntryResponse dto = new DepositRefundEntryResponse();
                    dto.setAmount(entry.getAmount());
                    dto.setReason(entry.getReason());
                    dto.setRefundedAt(entry.getRefundedAt());
                    return dto;
                })
                .toList();
    }
}