package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardSummaryResponse {
    private long totalProperties;
    private long totalUnits;
    private long occupiedUnits;
    private long activeLeases;
    private BigDecimal monthlyRevenue;
    private long pendingMaintenance;
}