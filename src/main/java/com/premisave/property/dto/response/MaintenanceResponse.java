package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class MaintenanceResponse {
    private String id;
    private String title;
    private String description;
    private String status;
}