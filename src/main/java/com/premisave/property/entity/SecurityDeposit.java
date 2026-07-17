package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import com.premisave.property.enums.DepositStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private DepositStatus status; // HELD, REFUNDED, DEDUCTED

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}