package com.premisave.property.dto.response;

import com.premisave.property.enums.UnitStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentalUnitSummaryResponse {
    private String id;
    private String unitNumber;
    private String floor;
    private BigDecimal rentAmount;
    private UnitStatus status;
}