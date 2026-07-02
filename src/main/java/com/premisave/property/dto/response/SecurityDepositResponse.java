package com.premisave.property.dto.response;

import com.premisave.property.enums.DepositStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SecurityDepositResponse {
    private String id;
    private String leaseId;
    private String tenantId;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private DepositStatus status;
    private LocalDateTime refundedAt;
}