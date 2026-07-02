package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundDepositRequest {

    @NotBlank
    private String leaseId;

    @NotNull
    private BigDecimal amount;

    private String reason;
}