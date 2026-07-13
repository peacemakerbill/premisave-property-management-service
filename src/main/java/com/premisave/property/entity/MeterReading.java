package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import com.premisave.property.enums.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "meter_readings")
public class MeterReading {

    @Id
    private String id;

    private String rentalUnitId;
    private String tenantId;

    private MeterType meterType;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal consumption;

    @CreatedDate
    private LocalDateTime readingDate;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}