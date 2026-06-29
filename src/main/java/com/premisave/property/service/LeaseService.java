package com.premisave.property.service;

import com.premisave.property.dto.request.CreateLeaseRequest;
import com.premisave.property.dto.response.LeaseResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.mapper.LeaseMapper;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.validation.LeaseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final LeaseMapper leaseMapper;
    private final LeaseValidator leaseValidator;

    @Transactional
    public LeaseResponse createLease(CreateLeaseRequest request) {
        leaseValidator.validateCreateLease(request);

        Lease lease = leaseMapper.toEntity(request);
        Lease saved = leaseRepository.save(lease);
        return leaseMapper.toResponse(saved);
    }
}