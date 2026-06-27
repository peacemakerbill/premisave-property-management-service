package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "tenants")
public class Tenant {

    @Id
    private String id;

    private String userId;           // Link to Auth Service
    private String fullName;
    private String phoneNumber;
    private String email;

    private Address currentAddress;
    private String occupation;
    private String idNumber;         // National ID / Passport

    private Boolean isActive = true;
    private Boolean isBlacklisted = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}