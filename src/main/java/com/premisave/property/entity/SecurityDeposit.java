package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import com.premisave.property.enums.DepositStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "security_deposits")
public class SecurityDeposit {

    @Id
    private String id;

    private String leaseId;        // set for lease-backed deposits
    private String rentalUnitId;   // set for direct (no-lease) unit deposits
    private String tenantId;

    private BigDecimal amount;
    private BigDecimal refundedAmount;

    private LocalDateTime refundedAt;

    private DepositStatus status; // HELD, REFUNDED, PARTIALLY_REFUNDED, DEDUCTED

    // Every refund call appends one entry here — the running total in
    // refundedAmount is always the sum of these amounts, kept in sync by
    // SecurityDepositService so callers never have to re-derive it.
    private List<DepositRefundEntry> refundHistory = new ArrayList<>();

    // Guards against a lost update if two refund requests for the same
    // deposit land at nearly the same time (MongoDB has no multi-document
    // transaction safety net here by default). Spring Data will throw
    // OptimisticLockingFailureException on a stale write instead of
    // silently letting one refund overwrite the other.
    @Version
    private Long version;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}