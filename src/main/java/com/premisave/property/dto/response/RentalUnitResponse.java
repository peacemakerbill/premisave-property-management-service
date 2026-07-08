package com.premisave.property.dto.response;

import com.premisave.property.enums.UnitStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentalUnitResponse {
    private String id;
    private String propertyId;
    private String unitNumber;
    private String floor;
    private BigDecimal rentAmount;
    private BigDecimal securityDeposit;
    private Boolean depositRequired;
    private UnitStatus status;
}