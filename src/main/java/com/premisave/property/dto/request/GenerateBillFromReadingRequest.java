package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerateBillFromReadingRequest {

    @NotBlank
    private String meterReadingId;

    /**
     * Optional override for the configured utility rate (USD per unit).
     * If omitted, the rate is resolved from the utility.rates config
     * based on the meter reading's utility type.
     */
    @Positive(message = "ratePerUnit must be greater than zero")
    @Digits(integer = 6, fraction = 6, message = "ratePerUnit must have at most 6 decimal places")
    private BigDecimal ratePerUnit;
}