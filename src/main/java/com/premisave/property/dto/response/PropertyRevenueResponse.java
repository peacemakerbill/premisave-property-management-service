package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertyRevenueResponse {
    private PropertySummaryResponse property;
    private BigDecimal totalRevenue;
    private int paymentCount;
}