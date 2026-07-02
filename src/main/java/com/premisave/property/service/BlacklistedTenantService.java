package com.premisave.property.service;

import com.premisave.property.dto.request.BlacklistTenantRequest;
import com.premisave.property.dto.response.BlacklistedTenantResponse;
import com.premisave.property.entity.BlacklistedTenant;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.BlacklistedTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlacklistedTenantService {

    private final BlacklistedTenantRepository blacklistedTenantRepository;
    private final TenantService tenantService;

    @Transactional
    public BlacklistedTenantResponse blacklistTenant(BlacklistTenantRequest request) {
        blacklistedTenantRepository.findByTenantId(request.getTenantId()).ifPresent(existing -> {
            throw new ConflictException("Tenant is already blacklisted");
        });

        BlacklistedTenant blacklisted = new BlacklistedTenant();
        blacklisted.setTenantId(request.getTenantId());
        blacklisted.setReason(request.getReason());
        blacklisted.setNotes(request.getNotes());
        blacklisted.setBlacklistedAt(LocalDateTime.now());

        BlacklistedTenant saved = blacklistedTenantRepository.save(blacklisted);

        tenantService.setBlacklisted(request.getTenantId(), true);

        return toResponse(saved);
    }

    @Transactional
    public void removeFromBlacklist(String tenantId) {
        BlacklistedTenant blacklisted = blacklistedTenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant is not currently blacklisted"));

        blacklistedTenantRepository.delete(blacklisted);
        tenantService.setBlacklisted(tenantId, false);
    }

    public BlacklistedTenantResponse getBlacklistEntry(String tenantId) {
        return blacklistedTenantRepository.findByTenantId(tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant is not currently blacklisted"));
    }

    public List<BlacklistedTenantResponse> getAllBlacklisted() {
        return blacklistedTenantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private BlacklistedTenantResponse toResponse(BlacklistedTenant blacklisted) {
        BlacklistedTenantResponse response = new BlacklistedTenantResponse();
        response.setTenantId(blacklisted.getTenantId());
        response.setReason(blacklisted.getReason());
        response.setBlacklistedAt(blacklisted.getBlacklistedAt());
        return response;
    }
}