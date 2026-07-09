package com.premisave.property.dto.response;

import com.premisave.property.enums.OccupancyType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OccupancyResponse {
    private String id;
    private OccupancyType occupancyType;
    private String rentalUnitId;
    private String propertyId;
    private String tenantId;
    private String leaseId;
    private LocalDateTime moveInDate;
    private LocalDateTime moveOutDate;
    private PropertySummaryResponse property;
    private RentalUnitSummaryResponse rentalUnit;
    private TenantSummaryResponse tenant;
}