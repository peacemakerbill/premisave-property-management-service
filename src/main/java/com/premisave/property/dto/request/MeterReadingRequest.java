package com.premisave.property.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MeterReadingRequest {
    private String rentalUnitId;
    private String meterType;
    private BigDecimal currentReading;
}