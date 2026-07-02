package com.premisave.property.dto.response;

import com.premisave.property.enums.PropertyType;
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
    private Double latitude;
    private Double longitude;
    private Integer totalUnits;
    private Integer availableUnits;
    private Boolean isActive;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}