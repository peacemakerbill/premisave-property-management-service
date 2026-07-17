package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DepositRefundEntryResponse {
    private BigDecimal amount;
    private String reason;
    private LocalDateTime refundedAt;
}