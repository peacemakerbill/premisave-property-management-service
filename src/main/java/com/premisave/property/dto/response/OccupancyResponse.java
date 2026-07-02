package com.premisave.property.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OccupancyResponse {
    private String id;
    private String rentalUnitId;
    private String tenantId;
    private String leaseId;
    private LocalDateTime moveInDate;
    private LocalDateTime moveOutDate;
}