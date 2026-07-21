package com.premisave.property.dto.response;

import com.premisave.property.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RentScheduleResponse {
    private String id;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private PaymentStatus status;

    // Derived, human-friendly payment info — computed at read time from
    // amountDue/amountPaid/status so it's always accurate, regardless of
    // how many payments have been applied to this schedule entry over time.
    private BigDecimal balanceDue;
    private BigDecimal overpaidAmount;
    private String paymentMessage;

    // Same lease/tenant/property/unit across every entry in a given
    // response list — resolved once per request in the service, not per
    // entry, since they're all for the same lease.
    private TenantSummaryResponse tenant;
    private LeaseSummaryResponse lease;
    private PropertySummaryResponse property;
    private RentalUnitSummaryResponse unit; // null for whole-property leases
}