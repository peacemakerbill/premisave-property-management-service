package com.premisave.property.validation;

import com.premisave.property.dto.request.CreateLeaseRequest;
import org.springframework.stereotype.Component;

@Component
public class LeaseValidator {

    public void validateCreateLease(CreateLeaseRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Lease start and end dates are required");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Lease start date must be before end date");
        }
        if (request.getMonthlyRent() == null || request.getMonthlyRent().doubleValue() <= 0) {
            throw new IllegalArgumentException("Monthly rent must be greater than zero");
        }
    }
}