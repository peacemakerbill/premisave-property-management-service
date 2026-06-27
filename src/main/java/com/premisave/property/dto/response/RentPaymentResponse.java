package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentPaymentResponse {
    private String id;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
}