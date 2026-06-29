package com.premisave.property.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantMovedInEvent {

    private String tenantId;
    private String rentalUnitId;
    private String leaseId;
    private LocalDateTime moveInDate;
}