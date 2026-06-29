package com.premisave.property.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {

    private String id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;           // CLIENT, HOME_OWNER, ADMIN, etc.
    private Boolean active;
    private Boolean verified;
    private String profilePictureUrl;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}