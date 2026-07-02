package com.premisave.property.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaintenanceResponse {
    private String id;
    private String tenantId;
    private String rentalUnitId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}