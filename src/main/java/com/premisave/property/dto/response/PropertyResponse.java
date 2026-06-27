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
    private Boolean isActive;
    private LocalDateTime createdAt;
}