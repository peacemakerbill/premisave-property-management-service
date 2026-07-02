package com.premisave.property.service;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.RentPayment;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.RentPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentPaymentService {

    private final RentPaymentRepository rentPaymentRepository;
    private final LeaseRepository leaseRepository;

    @Transactional
    public RentPaymentResponse recordPayment(RentPaymentRequest request, String tenantId) {
        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        RentPayment payment = new RentPayment();
        payment.setLeaseId(lease.getId());
        payment.setTenantId(tenantId);
        payment.setAmount(lease.getMonthlyRent());
        payment.setAmountPaid(request.getAmount());
        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus(request.getAmount().compareTo(lease.getMonthlyRent()) >= 0
                ? PaymentStatus.PAID
                : PaymentStatus.PARTIALLY_PAID);

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