package com.premisave.property.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentPaidEvent {

    private String leaseId;
    private String tenantId;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private String transactionId;
}