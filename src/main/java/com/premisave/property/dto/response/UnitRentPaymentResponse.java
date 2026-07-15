package com.premisave.property.dto.response;

import com.premisave.property.enums.PaymentMethod;
import com.premisave.property.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UnitRentPaymentResponse {
    private String id;
    private String rentalUnitId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal resultingBalance;
    private String description;
    private LocalDateTime paidAt;
}