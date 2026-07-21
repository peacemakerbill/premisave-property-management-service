package com.premisave.property.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TenantRentBalanceSummaryResponse {
    private String tenantId;
    private TenantSummaryResponse tenant;

    private BigDecimal totalArrearsOwed;
    private BigDecimal totalCreditAvailable;
    private BigDecimal netBalance; // positive = owes overall, negative = in credit overall

    private List<RentBalanceResponse> breakdown; // one entry per lease and per directly-occupied unit
}