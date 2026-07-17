package com.premisave.property.entity;

import com.premisave.property.enums.PaymentMethod;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.PaymentType;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A rent payment transaction against a directly-occupied (no-lease) rental
 * unit. Mirrors LeaseRentUnitPayment's role for leases, but scoped to
 * rentalUnitId instead of leaseId, and carries the resulting RentBalance
 * snapshot at the time of payment.
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

    private PaymentType paymentType = PaymentType.RENT;
    private BigDecimal depositAmountApplied = BigDecimal.ZERO;
    private BigDecimal rentAmountApplied = BigDecimal.ZERO;

    private PaymentStatus status; // PAID / PARTIALLY_PAID / OVERPAID

    // The tenant's RentBalance.balance immediately after this payment was
    // applied — positive means arrears remain, negative means credit.
    private BigDecimal resultingBalance;

    private String description;

    private LocalDateTime paidAt;

    @CreatedDate
    private LocalDateTime createdAt;
}