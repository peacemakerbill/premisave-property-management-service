package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "blacklisted_tenants")
public class BlacklistedTenant {

    @Id
    private String id;

    private String tenantId;
    private String reason;
    private String notes;

    private String rentalUnitId;   // unit the tenant was in at time of blacklisting, if known
    private String propertyId;     // property the tenant was in at time of blacklisting, if known

    private LocalDateTime blacklistedAt;
    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt;
}