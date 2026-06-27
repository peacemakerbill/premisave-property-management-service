package com.premisave.property.dto.request;

import lombok.Data;

@Data
public class UpdatePropertyRequest {
    private String title;
    private String description;
    private Boolean isActive;
}