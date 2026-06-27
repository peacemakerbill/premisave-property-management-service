package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOwnerRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phoneNumber;
    private String email;

    private AddressRequest address;
}