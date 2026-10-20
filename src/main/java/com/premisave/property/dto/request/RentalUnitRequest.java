package com.premisave.property.dto.request;

import com.premisave.property.enums.UnitStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentalUnitRequest {

    private String unitNumber;
    private String floor;

    @NotNull
    @Positive(message = "rentAmount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "rentAmount must have at most 2 decimal places")
    private BigDecimal rentAmount;

    @PositiveOrZero(message = "securityDeposit cannot be negative")

    @Digits(integer = 12, fraction = 2, message = "securityDeposit must have at most 2 decimal places")

    private BigDecimal securityDeposit;
    private Boolean depositRequired;
    private UnitStatus status;
}