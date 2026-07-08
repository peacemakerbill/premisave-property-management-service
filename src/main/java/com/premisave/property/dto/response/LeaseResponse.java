package com.premisave.property.dto.response;

import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.LeaseType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaseResponse {
    private String id;
    private String tenantId;
    private String rentalUnitId;
    private String propertyId;
    private LeaseType leaseType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private LeaseStatus status;
}