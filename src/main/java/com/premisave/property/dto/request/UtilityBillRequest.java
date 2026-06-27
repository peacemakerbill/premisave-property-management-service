package com.premisave.property.dto.request;

import com.premisave.property.enums.UtilityType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UtilityBillRequest {
    private String rentalUnitId;
    private UtilityType utilityType;
    private BigDecimal amount;
}