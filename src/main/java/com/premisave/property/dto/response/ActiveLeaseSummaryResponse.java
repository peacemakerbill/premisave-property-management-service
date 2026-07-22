package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class ActiveLeaseSummaryResponse {
    private LeaseSummaryResponse lease;
    private TenantSummaryResponse tenant;
    private PropertySummaryResponse property;
    private RentalUnitSummaryResponse unit; // null for whole-property leases
}