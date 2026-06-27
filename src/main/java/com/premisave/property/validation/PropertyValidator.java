package com.premisave.property.validation;

import com.premisave.property.dto.request.CreatePropertyRequest;
import org.springframework.stereotype.Component;

@Component
public class PropertyValidator {

    public void validateCreateProperty(CreatePropertyRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Property title is required");
        }
        if (request.getPropertyType() == null) {
            throw new IllegalArgumentException("Property type is required");
        }
        if (request.getAddress() == null) {
            throw new IllegalArgumentException("Property address is required");
        }
    }
}