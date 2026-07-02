package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class OccupancyReportResponse {
    private String ownerId;
    private long totalUnits;
    private long occupiedUnits;
    private long vacantUnits;
    private double occupancyRate;
}