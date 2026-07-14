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
}