package com.premisave.property.service;

import com.premisave.property.dto.request.CreateOwnerRequest;
import com.premisave.property.dto.response.OwnerResponse;
import com.premisave.property.entity.Owner;
import com.premisave.property.mapper.OwnerMapper;
import com.premisave.property.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final OwnerMapper ownerMapper;

    public OwnerResponse createOwner(CreateOwnerRequest request, String userId) {
        Owner owner = ownerMapper.toEntity(request);
        owner.setUserId(userId);
        Owner saved = ownerRepository.save(owner);
        return ownerMapper.toResponse(saved);
    }
}