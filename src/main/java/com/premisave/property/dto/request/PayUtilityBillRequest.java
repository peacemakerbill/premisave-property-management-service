package com.premisave.property.dto.request;

import com.premisave.property.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayUtilityBillRequest {

    @NotBlank
    private String billId;

    @NotNull
    private BigDecimal amount;

    // Optional. Only WALLET (or omitted) is accepted.
    private PaymentMethod paymentMethod;

    // Optional idempotency key, 1-40 chars of letters/digits/. _ : -
    // Reuse it when retrying so the tenant is never charged twice.
    private String reference;
}