package com.premisave.property.dto.response.auth_service;

import lombok.Data;

import java.time.LocalDateTime;

import com.premisave.property.dto.response.UserDto;

@Data
public class ReviewDto {
    private String id;
    private UserDto user;       // reviewer — best-effort mapping, some fields may be null
    private String targetId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}