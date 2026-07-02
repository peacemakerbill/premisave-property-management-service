package com.premisave.property.entity;

import com.premisave.property.enums.UnitStatus;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "rental_units")
public class RentalUnit {

    @Id
    private String id;

    private String propertyId;
    private String unitNumber;
    private String floor;

    private BigDecimal rentAmount;
    private BigDecimal securityDeposit;
    
    private Boolean depositRequired = true;

    private UnitStatus status = UnitStatus.VACANT;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}