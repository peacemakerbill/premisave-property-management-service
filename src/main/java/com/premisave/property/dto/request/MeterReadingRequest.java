package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

import com.premisave.property.enums.MeterType;

@Data
public class MeterReadingRequest {

    @NotBlank
    private String rentalUnitId;

    @NotNull
    private MeterType meterType;

    @NotNull
    private BigDecimal currentReading;
}