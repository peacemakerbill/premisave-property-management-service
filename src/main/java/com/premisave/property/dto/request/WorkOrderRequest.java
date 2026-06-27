package com.premisave.property.dto.request;

import lombok.Data;

@Data
public class WorkOrderRequest {
    private String maintenanceRequestId;
    private String assignedTo;
    private String title;
    private String description;
}