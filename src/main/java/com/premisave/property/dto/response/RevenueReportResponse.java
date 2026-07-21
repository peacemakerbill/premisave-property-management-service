package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RevenueReportResponse {
    private String ownerId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalRevenue;
    private int paymentCount;

    private OwnerSummaryResponse owner;
    private List<PropertyRevenueResponse> properties;
}