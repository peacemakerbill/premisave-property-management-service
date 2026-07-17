package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecurityDepositRequest {

    // Exactly one of leaseId or rentalUnitId must be provided.
    private String leaseId;

    // For direct (no-lease) unit deposits. tenantId is required alongside
    // it, since there's no Lease record to resolve the tenant from.
    private String rentalUnitId;
    private String tenantId;

    @NotNull
    private BigDecimal amount;
}