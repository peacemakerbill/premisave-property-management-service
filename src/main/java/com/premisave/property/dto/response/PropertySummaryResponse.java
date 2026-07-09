package com.premisave.property.dto.response;

import com.premisave.property.enums.PropertyType;
import lombok.Data;

@Data
public class PropertySummaryResponse {
    private String id;
    private String title;
    private PropertyType propertyType;
    private AddressResponse address;
    private String registrationNumber;
}