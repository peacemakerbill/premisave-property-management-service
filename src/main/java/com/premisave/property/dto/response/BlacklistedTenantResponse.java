package com.premisave.property.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlacklistedTenantResponse {
    private String tenantId;
    private String reason;
    private LocalDateTime blacklistedAt;
    private RentalUnitSummaryResponse rentalUnit;
    private PropertySummaryResponse property;
}