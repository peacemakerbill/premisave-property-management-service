package com.premisave.property.entity;

import com.premisave.property.enums.PaymentStatus;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "rent_payments")
public class RentPayment {

    @Id
    private String id;

    private String leaseId;
    private String tenantId;

    private BigDecimal amount;
    private BigDecimal amountPaid;

    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDateTime dueDate;
    private LocalDateTime paidAt;

    @CreatedDate
    private LocalDateTime createdAt;
}