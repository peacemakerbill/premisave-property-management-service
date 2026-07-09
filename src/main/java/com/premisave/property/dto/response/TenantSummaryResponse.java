package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class TenantSummaryResponse {
    private String id;
    private String fullName;
    private String phoneNumber;
    private String email;
}