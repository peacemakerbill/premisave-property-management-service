package com.premisave.property.dto.response;

import com.premisave.property.enums.WorkOrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderResponse {
    private String id;
    private String maintenanceRequestId;
    private String assignedTo;
    private String title;
    private String description;
    private WorkOrderStatus status;
    private LocalDateTime completedAt;
}