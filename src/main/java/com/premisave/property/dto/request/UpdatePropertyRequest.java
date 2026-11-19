package com.premisave.property.dto.request;

import com.premisave.property.enums.PropertyType;
import com.premisave.property.enums.RegistrationType;
import lombok.Data;

/** Every field is optional: only what is sent changes. */
@Data
public class UpdatePropertyRequest {
    private String title;
    private String description;
    private Boolean isActive;
    private PropertyType propertyType;
    private AddressRequest address;
    private Double latitude;
    private Double longitude;
    private String registrationNumber;
    private RegistrationType registrationType;
}
