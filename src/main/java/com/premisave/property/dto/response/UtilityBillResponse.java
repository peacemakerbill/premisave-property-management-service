package com.premisave.property.dto.response;

import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UtilityType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UtilityBillResponse {
    private String id;
    private String tenantId;
    private String rentalUnitId;
    private UtilityType utilityType;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private PaymentStatus status;
    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;
    private String sourceMeterReadingId;

    // Derived, human-friendly payment info — computed at read time from
    // amount/amountPaid/status so it's always accurate, not just right
    // after a payment call.
    private BigDecimal balanceDue;
    private BigDecimal overpaidAmount;
    private String paymentMessage;

    private TenantSummaryResponse tenant;
    private RentalUnitSummaryResponse rentalUnit;
    private PropertySummaryResponse property;
}