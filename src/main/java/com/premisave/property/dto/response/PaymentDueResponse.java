package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDueResponse {
    private String leaseId;
    private BigDecimal rentDue;
    private BigDecimal depositDue;
    private BigDecimal totalDue;
    private boolean depositRequired;
    private boolean depositAlreadyHeld;
}