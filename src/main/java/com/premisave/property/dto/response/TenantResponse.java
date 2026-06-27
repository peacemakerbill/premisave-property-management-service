package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class TenantResponse {
    private String id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String idNumber;
    private Boolean isBlacklisted;
}