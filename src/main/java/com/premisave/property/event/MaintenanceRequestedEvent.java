package com.premisave.property.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestedEvent {

    private String maintenanceId;
    private String tenantId;
    private String rentalUnitId;
    private String title;
    private LocalDateTime requestedAt;
}