package com.premisave.property.dto.response;

import com.premisave.property.enums.PaymentMethod;
import com.premisave.property.enums.PaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentPaymentResponse {
    private String id;
    private PaymentType paymentType;
    private BigDecimal amount;
    private BigDecimal rentAmountApplied;
    private BigDecimal depositAmountApplied;
    private String status;
    private PaymentMethod paymentMethod;
    private LocalDateTime paidAt;
    private String description;
}