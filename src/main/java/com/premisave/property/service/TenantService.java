package com.premisave.property.service;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.request.UpdateTenantRequest;
import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.TenantResponse;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.Tenant;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.TenantRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse registerTenant(TenantRegistrationRequest request, String userId) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("fullName is required");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new BadRequestException("phoneNumber is required");
        }

        tenantRepository.findByUserId(userId).ifPresent(existing -> {
            throw new ConflictException("A tenant profile already exists for this user");
        });

        if (request.getIdNumber() != null) {
            tenantRepository.findByIdNumber(request.getIdNumber()).ifPresent(existing -> {
                throw new ConflictException("A tenant with this ID number is already registered");
            });
        }
        tenantRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            throw new ConflictException("A tenant with this email is already registered");
        });

        Tenant tenant = new Tenant();
        tenant.setUserId(userId);
        tenant.setFullName(request.getFullName());
        tenant.setPhoneNumber(request.getPhoneNumber());
        tenant.setEmail(request.getEmail());
        tenant.setIdNumber(request.getIdNumber());
        tenant.setCurrentAddress(toAddress(request.getCurrentAddress()));
        tenant.setIsActive(true);
        tenant.setIsBlacklisted(false);

        return toResponse(tenantRepository.save(tenant));
    }

    public TenantResponse getTenantById(String id) {
        return toResponse(findTenantOrThrow(id));
    }

    public TenantResponse getTenantByUserId(String userId) {
        return toResponse(findTenantByUserIdOrThrow(userId));
    }

    @Transactional
    public TenantResponse updateTenantByUserId(UpdateTenantRequest request, String userId) {
        Tenant tenant = findTenantByUserIdOrThrow(userId);

        if (request.getFullName() != null) {
            tenant.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            tenant.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            tenant.setEmail(request.getEmail());
        }
        if (request.getOccupation() != null) {
            tenant.setOccupation(request.getOccupation());
        }

        return toResponse(tenantRepository.save(tenant));
    }

    // Called by BlacklistedTenantService to keep the flag on Tenant in sync
    @Transactional
    public void setBlacklisted(String tenantId, boolean blacklisted) {
        Tenant tenant = findTenantOrThrow(tenantId);
        tenant.setIsBlacklisted(blacklisted);
        tenantRepository.save(tenant);
    }

    private Tenant findTenantOrThrow(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private Tenant findTenantByUserIdOrThrow(String userId) {
        return tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found"));
    }

    private Address toAddress(com.premisave.property.dto.request.AddressRequest request) {
        if (request == null) {
            return null;
        }
        Address address = new Address();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        return address;
    }

    private AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setLandmark(address.getLandmark());
        return response;
    }

    private TenantResponse toResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setUserId(tenant.getUserId());
        response.setFullName(tenant.getFullName());
        response.setPhoneNumber(tenant.getPhoneNumber());
        response.setEmail(tenant.getEmail());
        response.setIdNumber(tenant.getIdNumber());
        response.setOccupation(tenant.getOccupation());
        response.setCurrentAddress(toAddressResponse(tenant.getCurrentAddress()));
        response.setIsActive(tenant.getIsActive());
        response.setIsBlacklisted(tenant.getIsBlacklisted());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }
}