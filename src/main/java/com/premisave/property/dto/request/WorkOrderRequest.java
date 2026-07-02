package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkOrderRequest {

    @NotBlank
    private String maintenanceRequestId;

    @NotBlank
    private String assignedTo;

    @NotBlank
    private String title;

    private String description;
}