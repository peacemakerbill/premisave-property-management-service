package com.premisave.property.dto.response.auth_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialActionResponse {
    private boolean success;
    private String message;
    private String action;
}