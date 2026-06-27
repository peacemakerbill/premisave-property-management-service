package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "owners")
public class Owner {

    @Id
    private String id;

    private String userId;           // Link to Auth Service user
    private String fullName;
    private String phoneNumber;
    private String email;

    private Address address;
    private BankDetails bankDetails;

    private Boolean isActive = true;
    private Boolean isVerified = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}