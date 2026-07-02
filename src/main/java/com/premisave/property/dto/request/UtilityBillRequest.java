package com.premisave.property.dto.request;

import com.premisave.property.enums.UtilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private BigDecimal amount;

    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;
}