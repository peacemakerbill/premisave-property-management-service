package com.premisave.property.service;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.entity.RentPayment;
import com.premisave.property.entity.RentSchedule;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.RentPaymentRepository;
import com.premisave.property.repository.RentScheduleRepository;
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

    @Transactional
    public RentPaymentResponse recordPayment(RentPaymentRequest request, String tenantId) {
        RentSchedule schedule = rentScheduleRepository
                .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(request.getLeaseId(),
                        List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE))
                .orElseThrow(() -> new ResourceNotFoundException("No outstanding rent due for this lease"));

        BigDecimal newAmountPaid = schedule.getAmountPaid().add(request.getAmount());
        schedule.setAmountPaid(newAmountPaid);
        schedule.setStatus(newAmountPaid.compareTo(schedule.getAmountDue()) >= 0
                ? PaymentStatus.PAID
                : PaymentStatus.PARTIALLY_PAID);
        rentScheduleRepository.save(schedule);

        RentPayment payment = new RentPayment();
        payment.setLeaseId(request.getLeaseId());
        payment.setTenantId(tenantId);
        payment.setAmount(schedule.getAmountDue());
        payment.setAmountPaid(request.getAmount());
        payment.setStatus(schedule.getStatus());
        payment.setDueDate(schedule.getDueDate().atStartOfDay());
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

    private RentPaymentResponse toResponse(RentPayment payment) {
        RentPaymentResponse response = new RentPaymentResponse();
        response.setId(payment.getId());
        response.setAmount(payment.getAmountPaid());
        response.setStatus(payment.getStatus().name());
        response.setPaidAt(payment.getPaidAt());
        return response;
    }
}