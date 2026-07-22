package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardSummaryResponse {
    private long totalProperties;
    private long totalUnits;
    private long occupiedUnits;
    private long activeLeases;
    private BigDecimal monthlyRevenue;
    private long pendingMaintenance;

    private OwnerSummaryResponse owner;

    // Drill-down data for the frontend's "click a card, see the detail"
    // dialogs. Covers the Properties/Units/Occupied cards (one breakdown,
    // since they're all facets of the same data), Active Leases, Monthly
    // Revenue, and Pending Maintenance.
    private List<PropertyOccupancyResponse> properties;
    private List<ActiveLeaseSummaryResponse> activeLeaseDetails;
    private List<PropertyRevenueResponse> revenueBreakdown;
    private List<MaintenanceSummaryResponse> pendingMaintenanceRequests;
}