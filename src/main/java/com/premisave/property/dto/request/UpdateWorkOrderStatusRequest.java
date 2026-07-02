package com.premisave.property.dto.request;

import com.premisave.property.enums.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateWorkOrderStatusRequest {

    @NotNull
    private WorkOrderStatus status;
}