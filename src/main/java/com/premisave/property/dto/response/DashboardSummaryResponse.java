package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardSummaryResponse {
    private long totalProperties;
    private long occupiedUnits;
    private BigDecimal monthlyRevenue;
    private int pendingMaintenance;
}