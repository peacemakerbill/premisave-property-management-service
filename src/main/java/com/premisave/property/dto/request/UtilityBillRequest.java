package com.premisave.property.dto.request;

import com.premisave.property.enums.UtilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UtilityBillRequest {

    @NotBlank
    private String rentalUnitId;

    @NotNull
    private UtilityType utilityType;

    @NotNull
    @Positive(message = "amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;

    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;
}