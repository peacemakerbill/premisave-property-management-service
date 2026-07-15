package com.premisave.property.dto.request;

import com.premisave.property.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentalUnitRentPaymentRequest {

    @NotBlank
    private String rentalUnitId;

    @NotNull
    private BigDecimal amount;

    // TODO(WALLET-INTEGRATION): same as lease rent payments — advisory
    // metadata only for now, no live gateway call happens from here.
    private PaymentMethod paymentMethod;
}