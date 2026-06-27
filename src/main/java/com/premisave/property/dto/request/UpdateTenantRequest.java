package com.premisave.property.dto.request;

import lombok.Data;

@Data
public class UpdateTenantRequest {
    private String fullName;
    private String phoneNumber;
    private String email;
    private String occupation;
}