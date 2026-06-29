package com.premisave.property.service;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.entity.RentPayment;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.mapper.RentPaymentMapper;
import com.premisave.property.repository.RentPaymentRepository;
import com.premisave.property.repository.LeaseRepository;
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
    private final RentPaymentMapper rentPaymentMapper;

    /**
     * Record a rent payment
     */
    @Transactional
    public RentPaymentResponse recordPayment(RentPaymentRequest request, String tenantId) {
        // Validate lease exists and belongs to tenant (in real app)
        RentPayment payment = rentPaymentMapper.toEntity(request);
        payment.setTenantId(tenantId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        RentPayment savedPayment = rentPaymentRepository.save(payment);

        // TODO: Publish RentPaidEvent to RabbitMQ for Wallet Service, etc.

        return rentPaymentMapper.toResponse(savedPayment);
    }

    /**
     * Get payment history for a lease
     */
    public List<RentPaymentResponse> getPaymentHistory(String leaseId) {
        return rentPaymentRepository.findById(leaseId)
                .stream()
                .map(rentPaymentMapper::toResponse)
                .toList();
    }

    /**
     * Check for overdue payments (can be scheduled)
     */
    public void checkOverduePayments() {
        // Logic to find overdue payments and trigger reminders
    }
}