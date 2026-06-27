package com.premisave.property.validation;

import com.premisave.property.dto.request.TenantRegistrationRequest;
import org.springframework.stereotype.Component;

@Component
public class TenantValidator {

    public void validateTenantRegistration(TenantRegistrationRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant full name is required");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
    }
}