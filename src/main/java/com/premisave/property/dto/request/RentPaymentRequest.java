package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentPaymentRequest {

    @NotNull
    private String leaseId;

    @NotNull
    private BigDecimal amount;
}