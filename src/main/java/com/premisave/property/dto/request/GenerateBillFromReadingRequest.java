package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerateBillFromReadingRequest {

    @NotBlank
    private String meterReadingId;

    @NotNull
    private BigDecimal ratePerUnit;
}