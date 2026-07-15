package com.premisave.property.entity;

import com.premisave.property.enums.PaymentMethod;
import com.premisave.property.enums.PaymentStatus;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A rent payment transaction against a directly-occupied (no-lease) rental
 * unit. Mirrors RentPayment's role for leases, but scoped to rentalUnitId
 * instead of leaseId, and carries the resulting RentBalance snapshot at the
 * time of payment.
 */
@Data
@Document(collection = "unit_rent_payments")
public class UnitRentPayment {

    @Id
    private String id;

    private String rentalUnitId;
    private String tenantId;
    private String propertyId;

    private BigDecimal amount;
    private PaymentMethod paymentMethod;

    private PaymentStatus status; // PAID / PARTIALLY_PAID / OVERPAID

    // The tenant's RentBalance.balance immediately after this payment was
    // applied — positive means arrears remain, negative means credit.
    private BigDecimal resultingBalance;

    private String description;

    private LocalDateTime paidAt;

    @CreatedDate
    private LocalDateTime createdAt;
}