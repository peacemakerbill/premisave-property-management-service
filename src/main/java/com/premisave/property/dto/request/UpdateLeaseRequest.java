package com.premisave.property.dto.request;

import com.premisave.property.enums.LeaseStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateLeaseRequest {
    private LocalDate endDate;
    private LeaseStatus status;
}