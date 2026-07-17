package com.premisave.property.service;

import com.premisave.property.client.WalletServiceClient;
import com.premisave.property.dto.request.RecordRentPaymentRequest;
import com.premisave.property.dto.request.UnitRentPaymentRequest;
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
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.exception.BadRequestException;
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
    private final WalletServiceClient walletServiceClient;
    private final EmailService emailService;
    private final SmsService smsService;

    // Post-payment side effects (wallet-service call, overpayment
    // notification — which includes a synchronous SMTP send via
    // EmailService) run here instead of on the request thread. SMTP
    // round-trips are the slowest part of this request by far; moving
    // them off-thread is what makes /api/v1/rent/units/pay fast. Reuses
    // the project's existing "taskExecutor" bean (see AsyncConfig) rather
    // than introducing a second thread pool.
    private final Executor taskExecutor;

    public UnitRentPaymentService(RentalUnitRepository rentalUnitRepository,
                                   OccupancyHistoryRepository occupancyHistoryRepository,
                                   UnitRentPaymentRepository unitRentPaymentRepository,
                                   TenantRepository tenantRepository,
                                   PropertyRepository propertyRepository,
                                   RentBalanceService rentBalanceService,
                                   WalletServiceClient walletServiceClient,
                                   EmailService emailService,
                                   SmsService smsService,
                                   @Qualifier("taskExecutor") Executor taskExecutor) {
        this.rentalUnitRepository = rentalUnitRepository;
        this.occupancyHistoryRepository = occupancyHistoryRepository;
        this.unitRentPaymentRepository = unitRentPaymentRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.rentBalanceService = rentBalanceService;
        this.walletServiceClient = walletServiceClient;
        this.emailService = emailService;
        this.smsService = smsService;
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    public UnitRentPaymentResponse recordPayment(UnitRentPaymentRequest request, String tenantId) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
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

        RentBalance balance = rentBalanceService.findOrCreateUnitBalance(
                unit.getId(), tenantId, unit.getPropertyId());

        rentBalanceService.chargeElapsedMonthsIfNeeded(balance, unit.getRentAmount());
        rentBalanceService.applyPayment(balance, request.getAmount());

        BigDecimal balanceAfter = balance.getBalance();
        PaymentStatus status = resolveStatus(balanceAfter);

        UnitRentPayment payment = new UnitRentPayment();
        payment.setRentalUnitId(unit.getId());
        payment.setTenantId(tenantId);
        payment.setPropertyId(unit.getPropertyId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(status);
        payment.setResultingBalance(balanceAfter);
        payment.setPaidAt(LocalDateTime.now());
        payment.setDescription(buildDescription(status, balanceAfter));

        UnitRentPayment saved = unitRentPaymentRepository.save(payment);

        // Fire-and-forget — never blocks the response. Failures inside
        // these are already logged individually; nothing further to
        // await here.
        taskExecutor.execute(() -> recordInWallet(saved));

        if (status == PaymentStatus.OVERPAID) {
            taskExecutor.execute(() -> notifyTenantOfOverpayment(tenantId, unit, balanceAfter.negate()));
        }

        return toResponse(saved, unit);
    }

    public List<UnitRentPaymentResponse> getPaymentHistory(String rentalUnitId) {
        return unitRentPaymentRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(payment -> toResponse(payment, null))
                .toList();
    }

    private PaymentStatus resolveStatus(BigDecimal balanceAfter) {
        int comparison = balanceAfter.compareTo(BigDecimal.ZERO);
        if (comparison == 0) return PaymentStatus.PAID;
        if (comparison < 0) return PaymentStatus.OVERPAID;
        return PaymentStatus.PARTIALLY_PAID;
    }

    private String buildDescription(PaymentStatus status, BigDecimal balanceAfter) {
        return switch (status) {
            case PAID -> "Rent payment received in full. Account is fully settled.";
            case OVERPAID -> "Rent payment received with an overpayment of KES " + balanceAfter.negate()
                    + " credited to your account. This credit will be applied automatically to your next "
                    + "rent charge, or contact your property owner regarding a refund.";
            case PARTIALLY_PAID -> "Partial rent payment received. KES " + balanceAfter
                    + " is still outstanding.";
            default -> "Rent payment recorded.";
        };
    }

    /**
     * Best-effort call to Wallet Service, mirroring LeaseRentPaymentService's
     * lease-based equivalent — failures are logged, never thrown. Runs on
     * taskExecutor, off the request thread.
     */
    private void recordInWallet(UnitRentPayment payment) {
        try {
            RecordRentPaymentRequest walletRequest = RecordRentPaymentRequest.builder()
                    .tenantId(payment.getTenantId())
                    .leaseId(null)
                    .propertyId(payment.getPropertyId())
                    .amount(payment.getAmount())
                    .paymentReference(payment.getId())
                    .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                    .paidAt(payment.getPaidAt())
                    .description("Rent payment for rental unit " + payment.getRentalUnitId())
                    .build();

            walletServiceClient.recordRentPayment(walletRequest);
        } catch (Exception e) {
            log.error("Failed to record unit rent payment {} in wallet-service (tenantId={}, unitId={}): {}",
                    payment.getId(), payment.getTenantId(), payment.getRentalUnitId(), e.getMessage());
        }
    }

    /**
     * Best-effort tenant notification about an overpayment credit — never
     * throws; a notification failure must not affect a payment already
     * booked. Runs on taskExecutor, off the request thread.
     */
    private void notifyTenantOfOverpayment(String tenantId, RentalUnit unit, BigDecimal creditAmount) {
        try {
            tenantRepository.findById(tenantId).ifPresentOrElse(tenant -> {
                String subject = "Overpayment Received — Credit on Your Account";
                String body = buildOverpaymentNotificationBody(tenant, creditAmount);

                boolean emailSent = emailService.sendNoticeEmail(
                        tenant.getEmail(), tenant.getFullName(), subject, "PAYMENT_UPDATE", body);
                boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(), subject + ": " + body);

                log.info("Overpayment notification sent for tenant {} on unit {} (emailSent={}, smsSent={})",
                        tenantId, unit.getId(), emailSent, smsSent);
            }, () -> log.warn("Could not send overpayment notification — tenant {} not found", tenantId));
        } catch (Exception e) {
            log.error("Failed to send overpayment notification for tenant {} on unit {}: {}",
                    tenantId, unit.getId(), e.getMessage());
        }
    }

    private String buildOverpaymentNotificationBody(Tenant tenant, BigDecimal creditAmount) {
        String name = tenant.getFullName() != null && !tenant.getFullName().isBlank()
                ? tenant.getFullName() : "there";
        return "Hi " + name + ",\n\nYour recent rent payment exceeded your current balance due. "
                + "An overpayment of KES " + creditAmount + " has been credited to your account and will "
                + "be applied automatically toward your next rent charge. Please contact your property "
                + "owner if you'd prefer a refund instead.\n\nThank you for your prompt payment.";
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    /**
     * @param unitHint the RentalUnit already loaded during recordPayment(),
     *                 passed through to avoid a redundant lookup. Pass null
     *                 (e.g. from getPaymentHistory()) to have it resolved
     *                 fresh from the payment's rentalUnitId.
     */
    private UnitRentPaymentResponse toResponse(UnitRentPayment payment, RentalUnit unitHint) {
        UnitRentPaymentResponse response = new UnitRentPaymentResponse();
        response.setId(payment.getId());
        response.setRentalUnitId(payment.getRentalUnitId());
        response.setAmount(payment.getAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
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
        summary.setRegistrationNumber(property.getRegistrationNumber());
        return summary;
    }
}