package com.premisave.property.dto.request;

import com.premisave.property.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMaintenanceStatusRequest {

    @NotNull
    private MaintenanceStatus status;
}