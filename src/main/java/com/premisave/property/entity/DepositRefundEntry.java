package com.premisave.property.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One line item in a SecurityDeposit's refund history. Embedded directly
// in the parent document — refunds are always read together with their
// deposit, so there's no need for a separate collection.
@Data
public class DepositRefundEntry {

    private BigDecimal amount;

    // Required when this entry represents a partial refund (see
    // SecurityDepositService#refundDeposit). Optional on a final/full refund.
    private String reason;

    private LocalDateTime refundedAt;
}