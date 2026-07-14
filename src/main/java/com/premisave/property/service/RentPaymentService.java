package com.premisave.property.service;

import com.premisave.property.client.WalletServiceClient;
import com.premisave.property.dto.request.RecordRentPaymentRequest;
import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.RentPayment;
import com.premisave.property.entity.RentSchedule;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.PaymentType;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.RentPaymentRepository;
import com.premisave.property.repository.RentScheduleRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.SecurityDepositRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentPaymentService {

    private final RentPaymentRepository rentPaymentRepository;
    private final RentScheduleRepository rentScheduleRepository;
    private final LeaseRepository leaseRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final SecurityDepositRepository securityDepositRepository;
    private final SecurityDepositService securityDepositService;
    private final WalletServiceClient walletServiceClient;

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
    public RentPaymentResponse recordPayment(RentPaymentRequest request, String tenantId) {
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

        BigDecimal rentApplied = BigDecimal.ZERO;
        PaymentStatus rentStatus = null;
        LocalDate scheduleDueDate = null;
        BigDecimal scheduleBalanceAfter = BigDecimal.ZERO;
        BigDecimal scheduleOverpaidAfter = BigDecimal.ZERO;

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            RentSchedule schedule = rentScheduleRepository
                    .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(request.getLeaseId(),
                            List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE))
                    .orElseThrow(() -> new ResourceNotFoundException("No outstanding rent due for this lease"));

            BigDecimal newAmountPaid = schedule.getAmountPaid().add(remaining);
            int comparison = newAmountPaid.compareTo(schedule.getAmountDue());

            PaymentStatus newScheduleStatus;
            if (comparison == 0) {
                newScheduleStatus = PaymentStatus.PAID;
            } else if (comparison > 0) {
                newScheduleStatus = PaymentStatus.OVERPAID;
            } else {
                newScheduleStatus = PaymentStatus.PARTIALLY_PAID;
            }

            schedule.setAmountPaid(newAmountPaid);
            schedule.setStatus(newScheduleStatus);
            rentScheduleRepository.save(schedule);

            rentApplied = remaining;
            rentStatus = newScheduleStatus;
            scheduleDueDate = schedule.getDueDate();
            scheduleBalanceAfter = comparison < 0 ? schedule.getAmountDue().subtract(newAmountPaid) : BigDecimal.ZERO;
            scheduleOverpaidAfter = comparison > 0 ? newAmountPaid.subtract(schedule.getAmountDue()) : BigDecimal.ZERO;
        }

        RentPayment payment = new RentPayment();
        payment.setLeaseId(request.getLeaseId());
        payment.setTenantId(tenantId);
        payment.setAmount(request.getAmount());
        payment.setAmountPaid(request.getAmount());
        payment.setDepositAmountApplied(depositApplied);
        payment.setRentAmountApplied(rentApplied);
        PaymentType paymentType = resolvePaymentType(depositApplied, rentApplied);
        payment.setPaymentType(paymentType);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(rentStatus != null ? rentStatus : PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setDescription(buildPaymentDescription(
                paymentType, depositApplied, rentApplied, rentStatus,
                scheduleDueDate, scheduleBalanceAfter, scheduleOverpaidAfter));

        RentPayment saved = rentPaymentRepository.save(payment);

        // Record the payment in Wallet Service for the tenant's transaction
        // history. Deliberately NOT part of the DB transaction above — a
        // wallet-service hiccup must never roll back or fail a rent payment
        // that has already been booked against the lease's rent schedule.
        recordInWallet(saved, lease.getPropertyId());

        return toResponse(saved);
    }

    public List<RentPaymentResponse> getPaymentHistory(String leaseId) {
        return rentPaymentRepository.findByLeaseId(leaseId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Best-effort call to Wallet Service. Failures are logged, not thrown —
     * the RentPayment row saved above is the source of truth for the payment.
     * TODO: once Wallet Service exposes a reconciliation/replay endpoint,
     * failed attempts here should be queued for retry instead of only logged.
     */
    private void recordInWallet(RentPayment payment, String propertyId) {
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

    private PaymentType resolvePaymentType(BigDecimal depositApplied, BigDecimal rentApplied) {
        boolean hasDeposit = depositApplied.compareTo(BigDecimal.ZERO) > 0;
        boolean hasRent = rentApplied.compareTo(BigDecimal.ZERO) > 0;
        if (hasDeposit && hasRent) return PaymentType.RENT_AND_DEPOSIT;
        if (hasDeposit) return PaymentType.SECURITY_DEPOSIT;
        return PaymentType.RENT;
    }

    /**
     * Builds a plain-language, point-in-time summary of what this specific
     * transaction did — including flagging an overpayment on the rent
     * schedule entry it was applied to. This is a snapshot, written once;
     * for the schedule's current live balance (which may have since changed
     * due to later payments), see RentScheduleResponse instead.
     */
    private String buildPaymentDescription(PaymentType paymentType, BigDecimal depositApplied, BigDecimal rentApplied,
                                            PaymentStatus rentStatus, LocalDate scheduleDueDate,
                                            BigDecimal scheduleBalanceAfter, BigDecimal scheduleOverpaidAfter) {
        StringBuilder message = new StringBuilder();

        if (depositApplied.compareTo(BigDecimal.ZERO) > 0) {
            message.append("KES ").append(depositApplied).append(" applied to your security deposit. ");
        }

        if (rentApplied.compareTo(BigDecimal.ZERO) > 0 && rentStatus != null) {
            message.append(switch (rentStatus) {
                case PAID -> "Rent payment of KES " + rentApplied + " received in full for the period due "
                        + scheduleDueDate + ".";
                case OVERPAID -> "Rent payment received with an overpayment of KES " + scheduleOverpaidAfter
                        + " above the amount due for the period due " + scheduleDueDate
                        + ". Please contact your property owner regarding a credit or refund.";
                case PARTIALLY_PAID -> "Partial rent payment received. KES " + scheduleBalanceAfter
                        + " is still outstanding for the period due " + scheduleDueDate + ".";
                default -> "Rent payment of KES " + rentApplied + " recorded for the period due "
                        + scheduleDueDate + ".";
            });
        } else if (paymentType == PaymentType.SECURITY_DEPOSIT) {
            message.append("No outstanding rent was covered by this payment.");
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

    private RentPaymentResponse toResponse(RentPayment payment) {
        RentPaymentResponse response = new RentPaymentResponse();
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
}