package com.premisave.property.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaseCreatedEvent {

    private String leaseId;
    private String tenantId;
    private String rentalUnitId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
}