package com.premisave.property.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordRentPaymentRequest {

    private String tenantId;
    private String leaseId;
    private String propertyId;
    private BigDecimal amount;
    private String paymentReference;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private String description;
}