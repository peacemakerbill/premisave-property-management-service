package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MeterReadingRequest {

    @NotBlank
    private String rentalUnitId;

    @NotBlank
    private String meterType;

    @NotNull
    private BigDecimal currentReading;
}