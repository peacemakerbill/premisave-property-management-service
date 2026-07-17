package com.premisave.property.dto.response;

import com.premisave.property.enums.DepositStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SecurityDepositResponse {
    private String id;
    private String leaseId;
    private String rentalUnitId;
    private String tenantId;
    private BigDecimal amount;
    private BigDecimal refundedAmount;

    // amount - refundedAmount — how much can still be refunded.
    private BigDecimal remainingRefundable;

    private DepositStatus status;
    private LocalDateTime refundedAt;
    private List<DepositRefundEntryResponse> refundHistory;
}