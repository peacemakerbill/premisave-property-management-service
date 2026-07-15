package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A running rent account balance for a directly-occupied (no-lease) rental
 * unit — modeled the way a school tracks a student's fee balance. Monthly
 * rent charges increase the balance; payments decrease it.
 *
 * Positive balance  = tenant owes this much in arrears.
 * Negative balance  = tenant has this much credit on file (overpayment),
 *                      automatically drawn down against future charges.
 * Zero balance      = fully settled.
 *
 * Lease-backed tenancies don't need this — their equivalent balance is
 * derived on the fly from RentSchedule entries (see RentBalanceService).
 * This entity exists only for direct occupancy, where no schedule rows
 * exist to derive a balance from.
 */
@Data
@Document(collection = "rent_balances")
public class RentBalance {

    @Id
    private String id;

    private String tenantId;
    private String rentalUnitId;
    private String propertyId;

    private BigDecimal balance = BigDecimal.ZERO;

    private LocalDateTime lastChargeAt;
    private BigDecimal lastChargeAmount;

    private LocalDateTime lastPaymentAt;
    private BigDecimal lastPaymentAmount;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}