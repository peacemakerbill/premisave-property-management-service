package com.premisave.property.dto.response;

import com.premisave.property.enums.MaintenanceStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaintenanceSummaryResponse {
    private String id;
    private String title;
    private String description;
    private MaintenanceStatus status;
    private LocalDateTime createdAt;

    private TenantSummaryResponse tenant;
    private PropertySummaryResponse property;
    private RentalUnitSummaryResponse unit;
}