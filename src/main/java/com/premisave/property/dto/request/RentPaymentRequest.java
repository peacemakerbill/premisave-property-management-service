package com.premisave.property.dto.request;

import com.premisave.property.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentPaymentRequest {

    @NotNull
    private String leaseId;

    @NotNull
    private BigDecimal amount;

    // TODO(WALLET-INTEGRATION): once the wallet service is wired in, this drives
    // which gateway the wallet service calls (M-Pesa STK Push, Stripe, PayPal).
    // For now it's advisory metadata only — no gateway call happens from here.
    private PaymentMethod paymentMethod;
}