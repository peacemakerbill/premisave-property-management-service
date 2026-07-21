package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class PropertyOccupancyResponse {
    private PropertySummaryResponse property;
    private long totalUnits;
    private long occupiedUnits;
    private long vacantUnits;
    private double occupancyRate;
}