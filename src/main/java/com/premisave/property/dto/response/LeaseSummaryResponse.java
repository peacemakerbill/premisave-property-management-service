package com.premisave.property.dto.response;

import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.LeaseType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaseSummaryResponse {
    private String id;
    private LeaseType leaseType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private LeaseStatus status;
}