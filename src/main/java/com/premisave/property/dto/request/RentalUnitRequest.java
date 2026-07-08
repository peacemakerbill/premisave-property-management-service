package com.premisave.property.dto.request;

import com.premisave.property.enums.UnitStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentalUnitRequest {

    private String unitNumber;
    private String floor;

    @NotNull
    private BigDecimal rentAmount;

    private BigDecimal securityDeposit;
    private Boolean depositRequired;
    private UnitStatus status;
}