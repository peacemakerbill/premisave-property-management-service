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
}