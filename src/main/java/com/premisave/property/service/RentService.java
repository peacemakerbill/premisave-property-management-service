package com.premisave.property.service;

import com.premisave.property.dto.request.RentPaymentRequest;
import com.premisave.property.dto.response.RentPaymentResponse;
import com.premisave.property.entity.RentPayment;
import com.premisave.property.mapper.RentPaymentMapper;
import com.premisave.property.repository.RentPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RentService {

    private final RentPaymentRepository rentPaymentRepository;
    private final RentPaymentMapper rentPaymentMapper;

    public RentPaymentResponse recordPayment(RentPaymentRequest request) {
        RentPayment payment = rentPaymentMapper.toEntity(request);
        RentPayment saved = rentPaymentRepository.save(payment);
        return rentPaymentMapper.toResponse(saved);
    }
}