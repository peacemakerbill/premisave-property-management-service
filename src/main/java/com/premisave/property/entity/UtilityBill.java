package com.premisave.property.entity;

import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UtilityType;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "utility_bills")
public class UtilityBill {

    @Id
    private String id;

    private String tenantId;
    private String rentalUnitId;

    private UtilityType utilityType;
    private BigDecimal amount;
    private BigDecimal amountPaid;

    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;

    // Set only when this bill was generated from a MeterReading.
    // Enforces one bill per reading — see UtilityBillingService.generateBillFromReading().
    private String sourceMeterReadingId;

    private PaymentStatus status = PaymentStatus.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;
}