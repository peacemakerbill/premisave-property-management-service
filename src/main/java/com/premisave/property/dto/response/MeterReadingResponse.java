package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MeterReadingResponse {
    private String id;
    private String rentalUnitId;
    private String meterType;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal consumption;
    private LocalDateTime readingDate;
}