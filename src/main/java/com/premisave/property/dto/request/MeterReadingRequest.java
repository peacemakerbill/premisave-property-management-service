package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
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
    @PositiveOrZero(message = "currentReading cannot be negative")
    @Digits(integer = 12, fraction = 3, message = "currentReading must have at most 3 decimal places")
    private BigDecimal currentReading;
}