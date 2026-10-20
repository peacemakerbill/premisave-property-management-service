package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
    @Positive(message = "monthlyRent must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "monthlyRent must have at most 2 decimal places")
    private BigDecimal monthlyRent;

    // Used only for whole-property leases (no RentalUnit to default from).
    // For unit leases, the unit's own securityDeposit is still used, as before.
    @PositiveOrZero(message = "securityDeposit cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "securityDeposit must have at most 2 decimal places")
    private BigDecimal securityDeposit;
}