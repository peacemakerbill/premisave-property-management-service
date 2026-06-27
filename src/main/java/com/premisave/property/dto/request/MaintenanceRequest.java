package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaintenanceRequest {

    @NotBlank
    private String rentalUnitId;

    @NotBlank
    private String title;

    private String description;
}