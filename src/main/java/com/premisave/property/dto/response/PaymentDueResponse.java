package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDueResponse {
    private String leaseId;       // set for lease-backed tenancies
    private String rentalUnitId;  // set for direct (no-lease) unit tenancies

    private BigDecimal rentDue;
    private BigDecimal depositDue;
    private BigDecimal totalDue;

    private boolean depositRequired;
    private boolean depositAlreadyHeld;
}