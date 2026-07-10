package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteInspectionRequest {

    @NotBlank
    private String findings;

    private String recommendations;
}