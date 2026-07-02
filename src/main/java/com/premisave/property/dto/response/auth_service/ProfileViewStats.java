package com.premisave.property.dto.response.auth_service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileViewStats {
    private long totalViews;
    private long viewsLast7Days;
    private long viewsLast30Days;
    private int uniqueViewers;
    private String message;
}