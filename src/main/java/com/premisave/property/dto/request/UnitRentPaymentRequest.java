package com.premisave.property.dto.request;

import com.premisave.property.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnitRentPaymentRequest {

    @NotBlank
    private String rentalUnitId;

    @NotNull
    private BigDecimal amount;

    // Optional. Payments are made from the tenant's Premisave wallet, so only
    // WALLET (or omitted) is accepted.
    private PaymentMethod paymentMethod;

    // Optional idempotency key, 1-40 chars of letters/digits/. _ : -
    // Reuse it when retrying so the tenant is never charged twice.
    private String reference;
}