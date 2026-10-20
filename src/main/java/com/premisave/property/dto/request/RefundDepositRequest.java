package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundDepositRequest {

    // Exactly one of leaseId or rentalUnitId must be provided.
    private String leaseId;

    private String rentalUnitId;
    private String tenantId;

    @NotNull
    @Positive(message = "amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;

    private String reason;
}