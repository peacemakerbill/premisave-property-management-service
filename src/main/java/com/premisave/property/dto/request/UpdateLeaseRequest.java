package com.premisave.property.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateLeaseRequest {
    private LocalDate endDate;
    private String status;
}