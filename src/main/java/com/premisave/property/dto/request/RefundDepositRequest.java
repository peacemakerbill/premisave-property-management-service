package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundDepositRequest {

    // Exactly one of leaseId or rentalUnitId must be provided.
    private String leaseId;

    private String rentalUnitId;
    private String tenantId;

    @NotNull
    private BigDecimal amount;

    private String reason;
}