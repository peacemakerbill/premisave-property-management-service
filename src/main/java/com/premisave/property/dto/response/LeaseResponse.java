package com.premisave.property.dto.response;

import com.premisave.property.enums.LeaseStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaseResponse {
    private String id;
    private String tenantId;
    private String rentalUnitId;
    private String propertyId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private LeaseStatus status;
}