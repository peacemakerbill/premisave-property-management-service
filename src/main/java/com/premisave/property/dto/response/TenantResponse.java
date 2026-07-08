package com.premisave.property.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantResponse {
    private String id;
    private String userId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String idNumber;
    private String occupation;
    private AddressResponse currentAddress;
    private Boolean isActive;
    private Boolean isBlacklisted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}