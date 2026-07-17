package com.premisave.property.service;

import com.premisave.property.client.WalletServiceClient;
import com.premisave.property.dto.request.LeaseRentPaymentRequest;
import com.premisave.property.dto.request.RecordRentPaymentRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.LeaseRentPaymentResponse;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.LeaseRentUnitPayment;
import com.premisave.property.entity.RentSchedule;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.PaymentType;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRentUnitPaymentRepository;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.RentScheduleRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.SecurityDepositRepository;
import com.premisave.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseRentUnitPaymentService {

    // Safety cap on how many schedule entries a single payment can cascade
    // across (~2 years of monthly rent). Guards against a runaway loop if
    // schedule data is ever malformed (e.g. a zero-amount entry).
    private static final int MAX_SCHEDULES_TO_APPLY_PER_PAYMENT = 24;

    private final LeaseRentUnitPaymentRepository leaseRentUnitPaymentRepository;
    private final RentScheduleRepository rentScheduleRepository;
    private final LeaseRepository leaseRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final SecurityDepositRepository securityDepositRepository;
    private final SecurityDepositService securityDepositService;
    private final WalletServiceClient walletServiceClient;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    // ------------------------------------------------------------------
    // TODO(WALLET-INTEGRATION):
    // This service still assumes money has ALREADY been collected by the
    // time recordPayment() is called — it books the transaction against rent
    // schedules / deposits, then records it in Wallet Service for the
    // tenant's statement history. It does NOT yet trigger a live checkout
    // (M-Pesa STK Push / Stripe / PayPal) — that would mean a separate
    // /initiate endpoint that calls wallet-service BEFORE recordPayment()
    // runs, with recordPayment() only firing after a payment provider
    // confirms success (webhook/callback or RabbitMQ event). Until then,
    // this is a manual/confirmed-payment booking endpoint, not a live
    // checkout flow.
    // ------------------------------------------------------------------

    public PaymentDueResponse getPaymentDue(String leaseId) {
        Lease lease = findLeaseOrThrow(leaseId);

        boolean depositRequired;
        boolean depositHeld = securityDepositRepository.findByLeaseId(leaseId).isPresent();
        BigDecimal depositAmount;

        if (lease.getRentalUnitId() != null) {
            RentalUnit unit = findUnitOrThrow(lease.getRentalUnitId());
            depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
            depositAmount = unit.getSecurityDeposit();
        } else {
            // Whole-property lease — deposit terms live directly on the Lease
            depositAmount = lease.getSecurityDeposit();
            depositRequired = depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        BigDecimal rentDue = rentScheduleRepository
                .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(leaseId,
                        List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE))
                .map(s -> s.getAmountDue().subtract(s.getAmountPaid()))
                .orElse(BigDecimal.ZERO);

        BigDecimal depositDue = (depositRequired && !depositHeld && depositAmount != null)
                ? depositAmount
                : BigDecimal.ZERO;

        PaymentDueResponse response = new PaymentDueResponse();
        response.setLeaseId(leaseId);
        response.setRentDue(rentDue);
        response.setDepositDue(depositDue);
        response.setTotalDue(rentDue.add(depositDue));
        response.setDepositRequired(depositRequired);
        response.setDepositAlreadyHeld(depositHeld);
        return response;
    }

    @Transactional
    public LeaseRentPaymentResponse recordPayment(LeaseRentPaymentRequest request, String tenantId) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        Lease lease = findLeaseOrThrow(request.getLeaseId());

        boolean depositRequired;
        BigDecimal depositAmount;

        if (lease.getRentalUnitId() != null) {
            RentalUnit unit = findUnitOrThrow(lease.getRentalUnitId());
            depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
            depositAmount = unit.getSecurityDeposit();
        } else {
            depositAmount = lease.getSecurityDeposit();
            depositRequired = depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        BigDecimal remaining = request.getAmount();
        BigDecimal depositApplied = BigDecimal.ZERO;

        boolean depositAlreadyHeld = securityDepositRepository.findByLeaseId(lease.getId()).isPresent();

        if (depositRequired && !depositAlreadyHeld
                && depositAmount != null
                && depositAmount.compareTo(BigDecimal.ZERO) > 0) {

            if (remaining.compareTo(depositAmount) < 0) {
                throw new BadRequestException(
                        "Payment of " + remaining + " is less than the required security deposit of "
                                + depositAmount + ". Deposit must be settled before or alongside rent.");
            }

            SecurityDepositRequest depositRequest = new SecurityDepositRequest();
            depositRequest.setLeaseId(lease.getId());
            depositRequest.setAmount(depositAmount);
            securityDepositService.holdDeposit(depositRequest);

            depositApplied = depositAmount;
            remaining = remaining.subtract(depositAmount);
        }

        RentApplicationResult rentResult = applyRentAcrossSchedules(request.getLeaseId(), remaining);

        LeaseRentUnitPayment payment = new LeaseRentUnitPayment();
        payment.setLeaseId(request.getLeaseId());
        payment.setTenantId(tenantId);
        payment.setAmount(request.getAmount());
        payment.setAmountPaid(request.getAmount());
        payment.setDepositAmountApplied(depositApplied);
        payment.setRentAmountApplied(rentResult.totalRentApplied());
        PaymentType paymentType = resolvePaymentType(depositApplied, rentResult.totalRentApplied());
        payment.setPaymentType(paymentType);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(rentResult.finalStatus() != null ? rentResult.finalStatus() : PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setDescription(buildPaymentDescription(paymentType, depositApplied, rentResult));

        LeaseRentUnitPayment saved = leaseRentUnitPaymentRepository.save(payment);

        // Record the payment in Wallet Service for the tenant's transaction
        // history. Deliberately NOT part of the DB transaction above — a
        // wallet-service hiccup must never roll back or fail a rent payment
        // that has already been booked against the lease's rent schedule.
        recordInWallet(saved, lease.getPropertyId());

        // Best-effort tenant notification — only when the payment spilled
        // into a future billing period or ended up as a pure credit (no
        // more schedule entries left to apply it to). An ordinary
        // exact/partial payment against the current period stays silent.
        if (rentResult.spilloverOccurred()) {
            notifyTenantOfOverpayment(tenantId, lease, rentResult);
        }

        return toResponse(saved);
    }

    public List<LeaseRentPaymentResponse> getPaymentHistory(String leaseId) {
        return leaseRentUnitPaymentRepository.findByLeaseId(leaseId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Rent application across schedule entries — carries an overpayment
    // forward into the next due period(s) instead of capping at the first.
    // ------------------------------------------------------------------

    /**
     * Applies {@code amountToApply} against the lease's outstanding rent
     * schedule entries, in due-date order. If a payment fully settles the
     * current period with money left over, the excess is applied to the
     * next outstanding period, and so on. If there's nothing left to apply
     * it to (e.g. the lease has ended), the excess is held as a visible
     * OVERPAID credit on the last schedule entry touched, rather than
     * silently discarded.
     */
    private RentApplicationResult applyRentAcrossSchedules(String leaseId, BigDecimal amountToApply) {
        List<ScheduleApplication> applications = new ArrayList<>();

        if (amountToApply.compareTo(BigDecimal.ZERO) <= 0) {
            return new RentApplicationResult(BigDecimal.ZERO, null, applications, false, BigDecimal.ZERO);
        }

        BigDecimal remaining = amountToApply;
        BigDecimal totalApplied = BigDecimal.ZERO;
        PaymentStatus finalStatus = null;
        RentSchedule lastTouchedSchedule = null;
        BigDecimal finalOverpaidAmount = BigDecimal.ZERO;
        int iterations = 0;

        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            if (++iterations > MAX_SCHEDULES_TO_APPLY_PER_PAYMENT) {
                log.warn("Rent payment for lease {} touched more than {} schedule entries — stopping to avoid "
                                + "a runaway loop. Remaining KES {} will be credited as an overpayment on the "
                                + "last schedule entry touched.",
                        leaseId, MAX_SCHEDULES_TO_APPLY_PER_PAYMENT, remaining);
                break;
            }

            Optional<RentSchedule> nextScheduleOpt = rentScheduleRepository
                    .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(leaseId,
                            List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE));

            if (nextScheduleOpt.isEmpty()) {
                break; // nothing left to apply to — handled below
            }

            RentSchedule schedule = nextScheduleOpt.get();
            BigDecimal due = schedule.getAmountDue().subtract(schedule.getAmountPaid());

            if (remaining.compareTo(due) >= 0) {
                // Fully settles this period, possibly with room left for the next one.
                schedule.setAmountPaid(schedule.getAmountDue());
                schedule.setStatus(PaymentStatus.PAID);
                rentScheduleRepository.save(schedule);

                applications.add(new ScheduleApplication(schedule.getDueDate(), due, PaymentStatus.PAID));
                totalApplied = totalApplied.add(due);
                remaining = remaining.subtract(due);
                finalStatus = PaymentStatus.PAID;
                lastTouchedSchedule = schedule;
            } else {
                BigDecimal newAmountPaid = schedule.getAmountPaid().add(remaining);
                schedule.setAmountPaid(newAmountPaid);
                schedule.setStatus(PaymentStatus.PARTIALLY_PAID);
                rentScheduleRepository.save(schedule);

                applications.add(new ScheduleApplication(schedule.getDueDate(), remaining, PaymentStatus.PARTIALLY_PAID));
                totalApplied = totalApplied.add(remaining);
                finalStatus = PaymentStatus.PARTIALLY_PAID;
                lastTouchedSchedule = schedule;
                remaining = BigDecimal.ZERO;
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            if (lastTouchedSchedule == null) {
                // No outstanding schedule existed at all for this lease.
                throw new ResourceNotFoundException("No outstanding rent due for this lease");
            }

            // No further schedule entries exist — hold the excess as a
            // visible credit on the last entry touched.
            BigDecimal newAmountPaid = lastTouchedSchedule.getAmountPaid().add(remaining);
            lastTouchedSchedule.setAmountPaid(newAmountPaid);
            lastTouchedSchedule.setStatus(PaymentStatus.OVERPAID);
            rentScheduleRepository.save(lastTouchedSchedule);

            applications.add(new ScheduleApplication(lastTouchedSchedule.getDueDate(), remaining, PaymentStatus.OVERPAID));
            totalApplied = totalApplied.add(remaining);
            finalStatus = PaymentStatus.OVERPAID;
            finalOverpaidAmount = remaining;
        }

        boolean spilloverOccurred = applications.size() > 1 || finalStatus == PaymentStatus.OVERPAID;

        return new RentApplicationResult(totalApplied, finalStatus, applications, spilloverOccurred, finalOverpaidAmount);
    }

    /**
     * Best-effort call to Wallet Service. Failures are logged, not thrown —
     * the LeaseRentUnitPayment row saved above is the source of truth for the
     * payment. TODO: once Wallet Service exposes a reconciliation/replay
     * endpoint, failed attempts here should be queued for retry instead of
     * only logged.
     */
    private void recordInWallet(LeaseRentUnitPayment payment, String propertyId) {
        try {
            RecordRentPaymentRequest walletRequest = RecordRentPaymentRequest.builder()
                    .tenantId(payment.getTenantId())
                    .leaseId(payment.getLeaseId())
                    .propertyId(propertyId)
                    .amount(payment.getAmountPaid())
                    .paymentReference(payment.getId())
                    .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                    .paidAt(payment.getPaidAt())
                    .description("Rent payment for lease " + payment.getLeaseId())
                    .build();

            walletServiceClient.recordRentPayment(walletRequest);
        } catch (Exception e) {
            log.error("Failed to record rent payment {} in wallet-service (tenantId={}, leaseId={}): {}",
                    payment.getId(), payment.getTenantId(), payment.getLeaseId(), e.getMessage());
        }
    }

    /**
     * Best-effort tenant notification (email + SMS) about an overpayment —
     * either money automatically applied to upcoming rent, or a pure credit
     * held because no further schedule entries exist. Never throws; a
     * notification failure must not affect a payment that's already booked.
     */
    private void notifyTenantOfOverpayment(String tenantId, Lease lease, RentApplicationResult rentResult) {
        try {
            tenantRepository.findById(tenantId).ifPresentOrElse(tenant -> {
                boolean isPureCredit = rentResult.finalStatus() == PaymentStatus.OVERPAID;
                String subject = isPureCredit
                        ? "Overpayment Received — Credit on Your Account"
                        : "Payment Applied to Upcoming Rent";

                String body = buildOverpaymentNotificationBody(tenant.getFullName(), isPureCredit, rentResult);

                boolean emailSent = emailService.sendNoticeEmail(
                        tenant.getEmail(), tenant.getFullName(), subject, "PAYMENT_UPDATE", body);

                boolean smsSent = smsService.sendNoticeSms(
                        tenant.getPhoneNumber(), subject + ": " + body);

                log.info("Overpayment notification sent for tenant {} on lease {} (emailSent={}, smsSent={})",
                        tenantId, lease.getId(), emailSent, smsSent);
            }, () -> log.warn("Could not send overpayment notification — tenant {} not found", tenantId));
        } catch (Exception e) {
            log.error("Failed to send overpayment notification for tenant {} on lease {}: {}",
                    tenantId, lease.getId(), e.getMessage());
        }
    }

    private String buildOverpaymentNotificationBody(String tenantName, boolean isPureCredit,
                                                      RentApplicationResult rentResult) {
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(tenantName != null && !tenantName.isBlank() ? tenantName : "there").append(",\n\n");

        if (isPureCredit) {
            body.append("Your recent rent payment exceeded the total amount currently due on your lease. ")
                    .append("An overpayment of KES ").append(rentResult.finalOverpaidAmount())
                    .append(" has been credited to your account. Please contact your property owner to arrange ")
                    .append("a refund, or to have it applied toward a future bill.\n");
        } else {
            body.append("Your recent rent payment was more than the amount due for your current billing period. ")
                    .append("The extra amount has been automatically applied toward your upcoming rent, ")
                    .append("covering the following period(s):\n");
            rentResult.applications().forEach(app ->
                    body.append("- ").append(app.dueDate()).append(": KES ").append(app.amountApplied())
                            .append(" (").append(app.resultingStatus()).append(")\n"));
        }

        body.append("\nThank you for your prompt payment.");
        return body.toString();
    }

    private PaymentType resolvePaymentType(BigDecimal depositApplied, BigDecimal rentApplied) {
        boolean hasDeposit = depositApplied.compareTo(BigDecimal.ZERO) > 0;
        boolean hasRent = rentApplied.compareTo(BigDecimal.ZERO) > 0;
        if (hasDeposit && hasRent) return PaymentType.RENT_AND_DEPOSIT;
        if (hasDeposit) return PaymentType.SECURITY_DEPOSIT;
        return PaymentType.RENT;
    }

    /**
     * Builds a plain-language, point-in-time summary of what this specific
     * transaction did — including a breakdown when the payment spilled
     * across multiple billing periods, and flagging any overpayment credit.
     * This is a snapshot, written once; for a schedule entry's current live
     * balance, see RentScheduleResponse instead.
     */
    private String buildPaymentDescription(PaymentType paymentType, BigDecimal depositApplied,
                                            RentApplicationResult rentResult) {
        StringBuilder message = new StringBuilder();

        if (depositApplied.compareTo(BigDecimal.ZERO) > 0) {
            message.append("KES ").append(depositApplied).append(" applied to your security deposit. ");
        }

        List<ScheduleApplication> applications = rentResult.applications();

        if (applications.isEmpty()) {
            if (paymentType == PaymentType.SECURITY_DEPOSIT) {
                message.append("No outstanding rent was covered by this payment.");
            }
            return message.toString().trim();
        }

        if (applications.size() == 1) {
            ScheduleApplication app = applications.get(0);
            message.append(switch (app.resultingStatus()) {
                case PAID -> "Rent payment of KES " + app.amountApplied() + " received in full for the period due "
                        + app.dueDate() + ".";
                case PARTIALLY_PAID -> "Partial rent payment of KES " + app.amountApplied()
                        + " received for the period due " + app.dueDate() + ".";
                case OVERPAID -> "Rent payment received with an overpayment of KES " + app.amountApplied()
                        + " credited above the amount due for the period due " + app.dueDate()
                        + ". Please contact your property owner regarding a credit or refund.";
                default -> "Rent payment of KES " + app.amountApplied() + " recorded for the period due "
                        + app.dueDate() + ".";
            });
        } else {
            message.append("Rent payment of KES ").append(rentResult.totalRentApplied())
                    .append(" applied across ").append(applications.size()).append(" billing periods: ");

            String breakdown = applications.stream()
                    .map(app -> switch (app.resultingStatus()) {
                        case PAID -> app.dueDate() + " (paid in full, KES " + app.amountApplied() + ")";
                        case PARTIALLY_PAID -> app.dueDate() + " (partially paid, KES " + app.amountApplied() + ")";
                        case OVERPAID -> app.dueDate() + " (overpaid by KES " + app.amountApplied() + ")";
                        default -> app.dueDate() + " (KES " + app.amountApplied() + ")";
                    })
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("");

            message.append(breakdown).append(".");

            if (rentResult.finalStatus() == PaymentStatus.OVERPAID) {
                message.append(" The excess of KES ").append(rentResult.finalOverpaidAmount())
                        .append(" has been credited as an overpayment. Please contact your property owner ")
                        .append("regarding a refund or credit toward a future bill.");
            } else {
                message.append(" The extra amount was automatically applied toward your upcoming rent.");
            }
        }

        return message.toString().trim();
    }

    private Lease findLeaseOrThrow(String leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
    }

    private RentalUnit findUnitOrThrow(String unitId) {
        return rentalUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));
    }

    private LeaseRentPaymentResponse toResponse(LeaseRentUnitPayment payment) {
        LeaseRentPaymentResponse response = new LeaseRentPaymentResponse();
        response.setId(payment.getId());
        response.setPaymentType(payment.getPaymentType());
        response.setAmount(payment.getAmountPaid());
        response.setRentAmountApplied(payment.getRentAmountApplied());
        response.setDepositAmountApplied(payment.getDepositAmountApplied());
        response.setStatus(payment.getStatus().name());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaidAt(payment.getPaidAt());
        response.setDescription(payment.getDescription());
        return response;
    }

    // ------------------------------------------------------------------
    // Internal result types for schedule application
    // ------------------------------------------------------------------

    private record ScheduleApplication(LocalDate dueDate, BigDecimal amountApplied, PaymentStatus resultingStatus) {
    }

    private record RentApplicationResult(
            BigDecimal totalRentApplied,
            PaymentStatus finalStatus,
            List<ScheduleApplication> applications,
            boolean spilloverOccurred,
            BigDecimal finalOverpaidAmount) {
    }
}