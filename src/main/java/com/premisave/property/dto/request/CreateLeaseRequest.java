package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateLeaseRequest {

    @NotBlank
    private String tenantId;

    // Provide exactly one of rentalUnitId or propertyId.
    private String rentalUnitId;
    private String propertyId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private BigDecimal monthlyRent;

    // Used only for whole-property leases (no RentalUnit to default from).
    // For unit leases, the unit's own securityDeposit is still used, as before.
    private BigDecimal securityDeposit;
}