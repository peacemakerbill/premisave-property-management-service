package com.premisave.property.dto.request;

import lombok.Data;

@Data
public class InspectionRequest {
    private String rentalUnitId;
    private String title;
    private String findings;
    private String recommendations;
}