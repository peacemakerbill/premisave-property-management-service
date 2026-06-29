package com.premisave.property.service;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.response.TenantResponse;
import com.premisave.property.entity.Tenant;
import com.premisave.property.mapper.TenantMapper;
import com.premisave.property.repository.TenantRepository;
import com.premisave.property.validation.TenantValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final TenantValidator tenantValidator;

    public TenantResponse registerTenant(TenantRegistrationRequest request) {
        tenantValidator.validateTenantRegistration(request);

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setIsActive(true);
        Tenant saved = tenantRepository.save(tenant);
        return tenantMapper.toResponse(saved);
    }
}