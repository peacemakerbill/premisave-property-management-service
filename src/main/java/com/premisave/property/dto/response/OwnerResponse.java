package com.premisave.property.dto.response;

import lombok.Data;

@Data
public class OwnerResponse {
    private String id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private AddressResponse address;
    private Boolean isActive;
}