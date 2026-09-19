package com.premisave.property.service;

import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.request.UnitRentPaymentRequest;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.dto.response.UnitRentPaymentResponse;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentBalance;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.entity.UnitRentPayment;
import com.premisave.property.entity.WalletTransfer;
import com.premisave.property.enums.PaymentMethod;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.PaymentType;
import com.premisave.property.enums.WalletTransferPurpose;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import com.premisave.property.repository.UnitRentPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class UnitRentPaymentService {

    private final RentalUnitRepository rentalUnitRepository;
    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final UnitRentPaymentRepository unitRentPaymentRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RentBalanceService rentBalanceService;
    private final SecurityDepositService securityDepositService;
    private final WalletPaymentService walletPaymentService;
    private final EmailService emailService;
    private final SmsService smsService;

    // Post-payment notifications (which include a synchronous SMTP send via
    // EmailService) run here instead of on the request thread. Reuses the
    // project's existing "taskExecutor" bean (see AsyncConfig).
    private final Executor taskExecutor;

    public UnitRentPaymentService(RentalUnitRepository rentalUnitRepository,
                                   OccupancyHistoryRepository occupancyHistoryRepository,
                                   UnitRentPaymentRepository unitRentPaymentRepository,
                                   TenantRepository tenantRepository,
                                   PropertyRepository propertyRepository,
                                   RentBalanceService rentBalanceService,
                                   SecurityDepositService securityDepositService,
                                   WalletPaymentService walletPaymentService,
                                   EmailService emailService,
                                   SmsService smsService,
                                   @Qualifier("taskExecutor") Executor taskExecutor) {
        this.rentalUnitRepository = rentalUnitRepository;
        this.occupancyHistoryRepository = occupancyHistoryRepository;
        this.unitRentPaymentRepository = unitRentPaymentRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.rentBalanceService = rentBalanceService;
        this.securityDepositService = securityDepositService;
        this.walletPaymentService = walletPaymentService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.taskExecutor = taskExecutor;
    }

    public PaymentDueResponse getPaymentDue(String rentalUnitId, String tenantId) {
        RentalUnit unit = rentalUnitRepository.findById(rentalUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));

        boolean depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
        BigDecimal depositAmount = unit.getSecurityDeposit();
        boolean depositHeld = securityDepositService.hasActiveDeposit(rentalUnitId, tenantId);
        BigDecimal depositDue = (depositRequired && !depositHeld && depositAmount != null)
                ? depositAmount
                : BigDecimal.ZERO;

        BigDecimal rentDue = rentBalanceService.getUnitBalance(rentalUnitId, tenantId).getArrearsOwed();

        PaymentDueResponse response = new PaymentDueResponse();
        response.setRentalUnitId(rentalUnitId);
        response.setRentDue(rentDue);
        response.setDepositDue(depositDue);
        response.setTotalDue(rentDue.add(depositDue));
        response.setDepositRequired(depositRequired);
        response.setDepositAlreadyHeld(depositHeld);

        response.setUnit(toRentalUnitSummary(unit));
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }
        if (unit.getPropertyId() != null) {
            propertyRepository.findById(unit.getPropertyId())
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }

        return response;
    }

    @Transactional
    public UnitRentPaymentResponse recordPayment(UnitRentPaymentRequest request, String tenantId) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }
        walletPaymentService.assertWalletPaymentMethod(request.getPaymentMethod());

        String reference = walletPaymentService.resolveReference(tenantId, request.getReference());

        // Idempotent replay: this reference was already paid and booked — return that payment,
        // don't touch the wallet or the rent balance again.
        Optional<UnitRentPayment> replay = unitRentPaymentRepository.findByPaymentReference(reference);
        if (replay.isPresent()) {
            UnitRentPayment existing = replay.get();
            boolean sameRequest = request.getRentalUnitId().equals(existing.getRentalUnitId())
                    && tenantId.equals(existing.getTenantId())
                    && request.getAmount().compareTo(existing.getAmount()) == 0;
            if (!sameRequest) {
                throw new ConflictException("This payment reference was already used for a different payment");
            }
            return toResponse(existing, null);
        }

        RentalUnit unit = rentalUnitRepository.findById(request.getRentalUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));

        if (unit.getRentAmount() == null) {
            throw new BadRequestException("This rental unit has no rent amount configured");
        }

        OccupancyHistory occupancy = occupancyHistoryRepository
                .findByRentalUnitIdAndTenantIdAndMoveOutDateIsNull(unit.getId(), tenantId)
                .orElseThrow(() -> new BadRequestException("You do not have an active occupancy on this unit"));

        if (occupancy.getLeaseId() != null) {
            throw new BadRequestException(
                    "This unit's occupancy is lease-backed. Use the lease rent payment endpoint instead.");
        }

        // ---- Validate everything BEFORE any money moves ----------------------------------------
        boolean depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
        BigDecimal depositAmount = unit.getSecurityDeposit();
        boolean depositAlreadyHeld = securityDepositService.hasActiveDeposit(unit.getId(), tenantId);

        BigDecimal depositToApply = BigDecimal.ZERO;
        if (depositRequired && !depositAlreadyHeld
                && depositAmount != null
                && depositAmount.compareTo(BigDecimal.ZERO) > 0) {

            if (request.getAmount().compareTo(depositAmount) < 0) {
                throw new BadRequestException(
                        "Payment of " + request.getAmount() + " is less than the required security deposit of "
                                + depositAmount + ". Deposit must be settled before or alongside rent.");
            }
            depositToApply = depositAmount;
        }

        BigDecimal rentPortion = request.getAmount().subtract(depositToApply);

        // Fetched once here so the description and both notifications below reuse the same lookup.
        Property property = propertyRepository.findById(unit.getPropertyId()).orElse(null);

        // ---- Move the money: tenant wallet -> property owner's wallet --------------------------
        WalletTransfer transfer = walletPaymentService.collect(new WalletPaymentService.CollectionCommand(
                reference, tenantId, unit.getPropertyId(), WalletTransferPurpose.UNIT_RENT, unit.getId(),
                request.getAmount(), "Rent payment for " + locationSummary(unit, property)));

        // ---- Book it ---------------------------------------------------------------------------
        UnitBooking booking;
        try {
            booking = bookUnitPayment(unit, tenantId, request.getAmount(), transfer.getReference(),
                    depositToApply, rentPortion);
        } catch (RuntimeException e) {
            throw walletPaymentService.bookingFailed(transfer, e);
        }

        UnitRentPayment saved = booking.payment();
        walletPaymentService.markBooked(transfer.getReference(), saved.getId());

        // Best-effort payment confirmation — fires for every successful
        // payment, exact/partial/overpaid alike. Separate from the
        // overpayment notice below, which carries different, more specific
        // messaging and only applies to the credit case.
        taskExecutor.execute(() -> notifyPaymentReceived(tenantId, unit, property, saved));

        if (booking.status() == PaymentStatus.OVERPAID) {
            BigDecimal creditAmount = booking.balanceAfter().negate();
            taskExecutor.execute(() -> notifyTenantOfOverpayment(tenantId, unit, property, creditAmount));
        }

        return toResponse(saved, unit);
    }

    public List<UnitRentPaymentResponse> getPaymentHistory(String rentalUnitId) {
        return unitRentPaymentRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(payment -> toResponse(payment, null))
                .toList();
    }

    /**
     * Local booking once the money has moved: hold the deposit portion, apply the
     * rent portion to the running RentBalance, and save the payment record.
     */
    private UnitBooking bookUnitPayment(RentalUnit unit, String tenantId, BigDecimal amount, String reference,
                                         BigDecimal depositToApply, BigDecimal rentPortion) {
        BigDecimal depositApplied = BigDecimal.ZERO;

        if (depositToApply.compareTo(BigDecimal.ZERO) > 0) {
            SecurityDepositRequest depositRequest = new SecurityDepositRequest();
            depositRequest.setRentalUnitId(unit.getId());
            depositRequest.setTenantId(tenantId);
            depositRequest.setAmount(depositToApply);
            securityDepositService.holdDeposit(depositRequest);
            depositApplied = depositToApply;
        }

        PaymentStatus status;
        BigDecimal balanceAfter;

        if (rentPortion.compareTo(BigDecimal.ZERO) > 0) {
            RentBalance balance = rentBalanceService.findOrCreateUnitBalance(
                    unit.getId(), tenantId, unit.getPropertyId());

            rentBalanceService.chargeElapsedMonthsIfNeeded(balance, unit.getRentAmount());
            rentBalanceService.applyPayment(balance, rentPortion);

            balanceAfter = balance.getBalance();
            status = resolveStatus(balanceAfter);
        } else {
            // The entire payment went to the deposit — no rent portion to apply.
            balanceAfter = rentBalanceService.getUnitBalance(unit.getId(), tenantId).getBalance();
            status = PaymentStatus.PAID;
        }

        PaymentType paymentType = resolvePaymentType(depositApplied, rentPortion);

        UnitRentPayment payment = new UnitRentPayment();
        payment.setRentalUnitId(unit.getId());
        payment.setTenantId(tenantId);
        payment.setPropertyId(unit.getPropertyId());
        payment.setAmount(amount);
        payment.setPaymentMethod(PaymentMethod.WALLET);
        payment.setPaymentReference(reference);
        payment.setPaymentType(paymentType);
        payment.setDepositAmountApplied(depositApplied);
        payment.setRentAmountApplied(rentPortion.compareTo(BigDecimal.ZERO) > 0 ? rentPortion : BigDecimal.ZERO);
        payment.setStatus(status);
        payment.setResultingBalance(balanceAfter);
        payment.setPaidAt(LocalDateTime.now());
        payment.setDescription(buildDescription(paymentType, depositApplied, status, balanceAfter));

        return new UnitBooking(unitRentPaymentRepository.save(payment), status, balanceAfter);
    }

    private PaymentStatus resolveStatus(BigDecimal balanceAfter) {
        int comparison = balanceAfter.compareTo(BigDecimal.ZERO);
        if (comparison == 0) return PaymentStatus.PAID;
        if (comparison < 0) return PaymentStatus.OVERPAID;
        return PaymentStatus.PARTIALLY_PAID;
    }

    private PaymentType resolvePaymentType(BigDecimal depositApplied, BigDecimal rentApplied) {
        boolean hasDeposit = depositApplied.compareTo(BigDecimal.ZERO) > 0;
        boolean hasRent = rentApplied.compareTo(BigDecimal.ZERO) > 0;
        if (hasDeposit && hasRent) return PaymentType.RENT_AND_DEPOSIT;
        if (hasDeposit) return PaymentType.SECURITY_DEPOSIT;
        return PaymentType.RENT;
    }

    private String buildDescription(PaymentType paymentType, BigDecimal depositApplied,
                                      PaymentStatus status, BigDecimal balanceAfter) {
        StringBuilder message = new StringBuilder();

        if (depositApplied.compareTo(BigDecimal.ZERO) > 0) {
            message.append("KES ").append(depositApplied).append(" applied to your security deposit. ");
        }

        if (paymentType == PaymentType.SECURITY_DEPOSIT) {
            message.append("No rent payment was included in this transaction.");
            return message.toString().trim();
        }

        message.append(switch (status) {
            case PAID -> "Rent payment received in full. Account is fully settled.";
            case OVERPAID -> "Rent payment received with an overpayment of KES " + balanceAfter.negate()
                    + " credited to your account. This credit will be applied automatically to your next "
                    + "rent charge, or contact your property owner regarding a refund.";
            case PARTIALLY_PAID -> "Partial rent payment received. KES " + balanceAfter
                    + " is still outstanding.";
            default -> "Rent payment recorded.";
        });

        return message.toString().trim();
    }

    /**
     * Best-effort payment confirmation (email + SMS) — fires for every
     * successful payment regardless of whether it was exact, partial, or
     * overpaid. Kept separate from notifyTenantOfOverpayment, which only
     * covers the credit case and carries different messaging; a payment
     * that triggers both methods sends two distinct notices, by design.
     * Never throws; a notification failure must not affect a payment
     * that's already booked. Runs on taskExecutor, off the request thread.
     */
    private void notifyPaymentReceived(String tenantId, RentalUnit unit, Property property, UnitRentPayment payment) {
        try {
            tenantRepository.findById(tenantId).ifPresentOrElse(tenant -> {
                String subject = "Payment Received";
                String body = buildPaymentReceivedBody(tenant, unit, property, payment);

                boolean emailSent = emailService.sendNoticeEmail(
                        tenant.getEmail(), tenant.getFullName(), subject, "PAYMENT_RECEIVED", body);

                boolean smsSent = smsService.sendNoticeSms(
                        tenant.getPhoneNumber(), subject + " — " + locationSummary(unit, property)
                                + ": KES " + payment.getAmount() + " received.");

                log.info("Payment confirmation sent for payment {} on unit {} (emailSent={}, smsSent={})",
                        payment.getId(), unit.getId(), emailSent, smsSent);
            }, () -> log.warn("Could not send payment confirmation — tenant {} not found", tenantId));
        } catch (Exception e) {
            log.error("Failed to send payment confirmation for payment {} on unit {}: {}",
                    payment.getId(), unit.getId(), e.getMessage());
        }
    }

    private String buildPaymentReceivedBody(Tenant tenant, RentalUnit unit, Property property,
                                             UnitRentPayment payment) {
        String name = tenant.getFullName() != null && !tenant.getFullName().isBlank()
                ? tenant.getFullName() : "there";
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(name).append(",\n\n");
        body.append("We've received your payment of KES ").append(payment.getAmount())
                .append(" for ").append(locationSummary(unit, property)).append(".\n\n");
        if (payment.getDescription() != null && !payment.getDescription().isBlank()) {
            body.append(payment.getDescription()).append("\n");
        }
        body.append("\nThank you for your payment.");
        return body.toString();
    }

    /**
     * Best-effort tenant notification about an overpayment credit — never
     * throws; a notification failure must not affect a payment already
     * booked. Runs on taskExecutor, off the request thread.
     */
    private void notifyTenantOfOverpayment(String tenantId, RentalUnit unit, Property property,
                                            BigDecimal creditAmount) {
        try {
            tenantRepository.findById(tenantId).ifPresentOrElse(tenant -> {
                String subject = "Overpayment Received — Credit on Your Account";
                String body = buildOverpaymentNotificationBody(tenant, unit, property, creditAmount);

                boolean emailSent = emailService.sendNoticeEmail(
                        tenant.getEmail(), tenant.getFullName(), subject, "PAYMENT_UPDATE", body);
                boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(),
                        subject + " (" + locationSummary(unit, property) + "): " + body);

                log.info("Overpayment notification sent for tenant {} on unit {} (emailSent={}, smsSent={})",
                        tenantId, unit.getId(), emailSent, smsSent);
            }, () -> log.warn("Could not send overpayment notification — tenant {} not found", tenantId));
        } catch (Exception e) {
            log.error("Failed to send overpayment notification for tenant {} on unit {}: {}",
                    tenantId, unit.getId(), e.getMessage());
        }
    }

    private String buildOverpaymentNotificationBody(Tenant tenant, RentalUnit unit, Property property,
                                                      BigDecimal creditAmount) {
        String name = tenant.getFullName() != null && !tenant.getFullName().isBlank()
                ? tenant.getFullName() : "there";
        return "Hi " + name + ",\n\nRegarding your account for " + locationSummary(unit, property) + ":\n\n"
                + "Your recent rent payment exceeded your current balance due. "
                + "An overpayment of KES " + creditAmount + " has been credited to your account and will "
                + "be applied automatically toward your next rent charge. Please contact your property "
                + "owner if you'd prefer a refund instead.\n\nThank you for your prompt payment.";
    }

    /**
     * Human-readable "which property/unit is this about" fragment, used in
     * the wallet transfer description and in notification emails/SMS so a
     * tenant on more than one unit can tell them apart at a glance. Falls
     * back gracefully if the property lookup came back empty.
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
    // Mapping
    // ------------------------------------------------------------------

    private UnitRentPaymentResponse toResponse(UnitRentPayment payment, RentalUnit unitHint) {
        UnitRentPaymentResponse response = new UnitRentPaymentResponse();
        response.setId(payment.getId());
        response.setRentalUnitId(payment.getRentalUnitId());
        response.setAmount(payment.getAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentReference(payment.getPaymentReference());
        response.setPaymentType(payment.getPaymentType());
        response.setDepositAmountApplied(payment.getDepositAmountApplied());
        response.setRentAmountApplied(payment.getRentAmountApplied());
        response.setStatus(payment.getStatus());
        response.setResultingBalance(payment.getResultingBalance());
        response.setDescription(payment.getDescription());
        response.setPaidAt(payment.getPaidAt());

        if (payment.getTenantId() != null) {
            tenantRepository.findById(payment.getTenantId())
                    .ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }

        RentalUnit unit = unitHint != null
                ? unitHint
                : (payment.getRentalUnitId() != null
                        ? rentalUnitRepository.findById(payment.getRentalUnitId()).orElse(null)
                        : null);

        if (unit != null) {
            response.setRentalUnit(toRentalUnitSummary(unit));

            if (unit.getPropertyId() != null) {
                propertyRepository.findById(unit.getPropertyId())
                        .ifPresent(property -> response.setProperty(toPropertySummary(property)));
            }
        }

        return response;
    }

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        TenantSummaryResponse summary = new TenantSummaryResponse();
        summary.setId(tenant.getId());
        summary.setFullName(tenant.getFullName());
        summary.setPhoneNumber(tenant.getPhoneNumber());
        summary.setEmail(tenant.getEmail());
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

    private PropertySummaryResponse toPropertySummary(Property property) {
        PropertySummaryResponse summary = new PropertySummaryResponse();
        summary.setId(property.getId());
        summary.setTitle(property.getTitle());
        summary.setPropertyType(property.getPropertyType());
        summary.setAddress(toAddressResponse(property.getAddress()));
        summary.setRegistrationNumber(property.getRegistrationNumber());
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

    private record UnitBooking(UnitRentPayment payment, PaymentStatus status, BigDecimal balanceAfter) {
    }
}