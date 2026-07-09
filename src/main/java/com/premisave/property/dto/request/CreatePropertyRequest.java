package com.premisave.property.dto.request;

import com.premisave.property.enums.PropertyType;
import com.premisave.property.enums.RegistrationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePropertyRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private PropertyType propertyType;

    @NotNull
    private AddressRequest address;

    private Double latitude;
    private Double longitude;

    @NotBlank
    private String registrationNumber;

    @NotNull
    private RegistrationType registrationType;
}