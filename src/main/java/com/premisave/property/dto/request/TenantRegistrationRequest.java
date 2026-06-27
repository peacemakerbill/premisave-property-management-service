package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRegistrationRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String email;

    private String idNumber;
    private AddressRequest currentAddress;
}