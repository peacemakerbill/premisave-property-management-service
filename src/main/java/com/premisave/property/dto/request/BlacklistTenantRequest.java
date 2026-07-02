package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlacklistTenantRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String reason;

    private String notes;
}