package com.premisave.property.dto.response.auth_service;

import lombok.Data;

@Data
public class UserInteractionDto {
    private long followerCount;
    private long followingCount;
    private long likeCount;
    private double averageRating;
    private int totalReviews;
}