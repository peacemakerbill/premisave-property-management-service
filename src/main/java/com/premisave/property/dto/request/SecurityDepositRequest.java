package com.premisave.property.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecurityDepositRequest {
    private String leaseId;
    private BigDecimal amount;
}