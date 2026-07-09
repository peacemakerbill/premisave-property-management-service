package com.premisave.property.dto.response;

import com.premisave.property.enums.PropertyType;
import com.premisave.property.enums.RegistrationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PropertyResponse {
    private String id;
    private String ownerId;
    private String title;
    private String description;
    private PropertyType propertyType;
    private AddressResponse address;
    private String registrationNumber;
    private RegistrationType registrationType;
    private Double latitude;
    private Double longitude;
    private Integer totalUnits;
    private Integer availableUnits;
    private Boolean isActive;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}