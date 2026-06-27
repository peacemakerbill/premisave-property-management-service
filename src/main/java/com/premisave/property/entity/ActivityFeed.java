package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "activity_feeds")
public class ActivityFeed {

    @Id
    private String id;

    private String userId;           // Tenant or Owner
    private String entityType;
    private String entityId;
    private String action;

    private String description;
    private String metadata;         // JSON string for extra info

    @CreatedDate
    private LocalDateTime timestamp;
}