package com.premisave.property.dto.response.auth_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileViewStats {
    private long totalViews;
    private String message;
}