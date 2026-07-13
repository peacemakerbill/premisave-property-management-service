package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerateBillFromReadingRequest {

    @NotBlank
    private String meterReadingId;

    /**
     * Optional override for the configured utility rate (KES per unit).
     * If omitted, the rate is resolved from the utility.rates config
     * based on the meter reading's utility type.
     */
    private BigDecimal ratePerUnit;
}