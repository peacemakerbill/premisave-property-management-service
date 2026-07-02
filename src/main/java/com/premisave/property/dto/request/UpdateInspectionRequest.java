package com.premisave.property.dto.request;

import lombok.Data;

@Data
public class UpdateInspectionRequest {
    private String findings;
    private String recommendations;
}