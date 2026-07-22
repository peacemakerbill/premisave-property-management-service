package com.premisave.property.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ProfileSyncStatusResponse {
    private boolean inSync;
    private List<String> outOfSyncFields;

    // The values currently on file in auth-service — what a Sync call
    // would apply if triggered right now, so the frontend can preview the
    // change before the user confirms it.
    private String latestFullName;
    private String latestPhoneNumber;
    private String latestEmail;
}