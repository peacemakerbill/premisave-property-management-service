package com.premisave.property.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentPaymentService {

    private final RentPaymentRepository rentPaymentRepository;
    private final RentScheduleRepository rentScheduleRepository;
    private final LeaseRepository leaseRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final SecurityDepositRepository securityDepositRepository;
    private final SecurityDepositService securityDepositService;

    // ------------------------------------------------------------------
    // TODO(WALLET-INTEGRATION):
    // This service currently assumes money has ALREADY been collected by the
    // time recordPayment() is called — it only books the transaction against
    // rent schedules / deposits. Once the wallet service is connected:
    //   1. Tenant hits POST /api/v1/rent/pay (or a new /initiate endpoint)
    //   2. This service (or a new PaymentGatewayService) calls the wallet
    //      service, which triggers M-Pesa STK Push / Stripe / PayPal
    //      depending on RentPaymentRequest.paymentMethod
    //   3. Wallet service confirms success asynchronously (webhook/callback
    //      or RabbitMQ event — matches the existing RentPaidEvent TODO below)
    //   4. Only on confirmed success should recordPayment() actually run
    // Until then, recordPayment() is effectively a manual/confirmed-payment
    // booking endpoint, not a live checkout.
    // ------------------------------------------------------------------

    public PaymentDueResponse getPaymentDue(String leaseId) {
        Lease lease = findLeaseOrThrow(leaseId);
        RentalUnit unit = findUnitOrThrow(lease.getRentalUnitId());

        boolean depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
        boolean depositHeld = securityDepositRepository.findByLeaseId(leaseId).isPresent();

        BigDecimal rentDue = rentScheduleRepository
                .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(leaseId,
                        List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE))
                .map(s -> s.getAmountDue().subtract(s.getAmountPaid()))
                .orElse(BigDecimal.ZERO);

        BigDecimal depositDue = (depositRequired && !depositHeld && unit.getSecurityDeposit() != null)
                ? unit.getSecurityDeposit()
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
        Lease lease = findLeaseOrThrow(request.getLeaseId());
        RentalUnit unit = findUnitOrThrow(lease.getRentalUnitId());

        BigDecimal remaining = request.getAmount();
        BigDecimal depositApplied = BigDecimal.ZERO;

        boolean depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
        boolean depositAlreadyHeld = securityDepositRepository.findByLeaseId(lease.getId()).isPresent();

        if (depositRequired && !depositAlreadyHeld
                && unit.getSecurityDeposit() != null
                && unit.getSecurityDeposit().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal depositDue = unit.getSecurityDeposit();
            if (remaining.compareTo(depositDue) < 0) {
                throw new BadRequestException(
                        "Payment of " + remaining + " is less than the required security deposit of "
                                + depositDue + ". Deposit must be settled before or alongside rent.");
            }

            SecurityDepositRequest depositRequest = new SecurityDepositRequest();
            depositRequest.setLeaseId(lease.getId());
            depositRequest.setAmount(depositDue);
            securityDepositService.holdDeposit(depositRequest);

            depositApplied = depositDue;
            remaining = remaining.subtract(depositDue);
        }

        BigDecimal rentApplied = BigDecimal.ZERO;
        PaymentStatus rentStatus = null;

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            RentSchedule schedule = rentScheduleRepository
                    .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(request.getLeaseId(),
                            List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE))
                    .orElseThrow(() -> new ResourceNotFoundException("No outstanding rent due for this lease"));

            BigDecimal newAmountPaid = schedule.getAmountPaid().add(remaining);
            schedule.setAmountPaid(newAmountPaid);
            schedule.setStatus(newAmountPaid.compareTo(schedule.getAmountDue()) >= 0
                    ? PaymentStatus.PAID
                    : PaymentStatus.PARTIALLY_PAID);
            rentScheduleRepository.save(schedule);

            rentApplied = remaining;
            rentStatus = schedule.getStatus();
        }

        RentPayment payment = new RentPayment();
        payment.setLeaseId(request.getLeaseId());
        payment.setTenantId(tenantId);
        payment.setAmount(request.getAmount());
        payment.setAmountPaid(request.getAmount());
        payment.setDepositAmountApplied(depositApplied);
        payment.setRentAmountApplied(rentApplied);
        payment.setPaymentType(resolvePaymentType(depositApplied, rentApplied));
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(rentStatus != null ? rentStatus : PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        RentPayment saved = rentPaymentRepository.save(payment);

        // TODO: publish RentPaidEvent to RabbitMQ for Wallet Service, etc.

        return toResponse(saved);
    }

    public List<RentPaymentResponse> getPaymentHistory(String leaseId) {
        return rentPaymentRepository.findByLeaseId(leaseId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentType resolvePaymentType(BigDecimal depositApplied, BigDecimal rentApplied) {
        boolean hasDeposit = depositApplied.compareTo(BigDecimal.ZERO) > 0;
        boolean hasRent = rentApplied.compareTo(BigDecimal.ZERO) > 0;
        if (hasDeposit && hasRent) return PaymentType.RENT_AND_DEPOSIT;
        if (hasDeposit) return PaymentType.SECURITY_DEPOSIT;
        return PaymentType.RENT;
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
        return response;
    }
}