package com.premisave.property.dto.request;

import com.premisave.property.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaseRentPaymentRequest {

    @NotNull
    private String leaseId;

    @NotNull
    private BigDecimal amount;

    // Optional. Payments are made from the tenant's Premisave wallet, so only
    // WALLET (or omitted) is accepted — top up the wallet via M-Pesa/Stripe/
    // PayPal in wallet-service first.
    private PaymentMethod paymentMethod;

    // Optional idempotency key, 1-40 chars of letters/digits/. _ : -
    // Generate one per payment attempt and REUSE it when retrying after a
    // timeout or error: the tenant is never charged twice for the same reference.
    private String reference;
}