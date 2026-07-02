package com.premisave.property.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InspectionResponse {
    private String id;
    private String rentalUnitId;
    private String inspectorId;
    private String title;
    private String findings;
    private String recommendations;
    private LocalDateTime inspectionDate;
}