package com.premisave.property.entity;

import com.premisave.property.enums.PropertyType;
import com.premisave.property.enums.RegistrationType;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "properties")
public class Property {

    @Id
    private String id;

    private String ownerId;                    // Reference to Owner
    private String title;
    private String description;

    private PropertyType propertyType;
    private Address address;
    private GeoLocation location;

    @Indexed(unique = true)
    private String registrationNumber;         // Title deed / LR number / lease certificate no.
    private RegistrationType registrationType;

    private Integer totalUnits;
    private Integer availableUnits;

    private Boolean isActive = true;
    private Boolean isVerified = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}