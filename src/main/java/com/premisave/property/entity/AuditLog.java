package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String entityType;
    private String entityId;
    private String action;           // CREATE, UPDATE, DELETE, etc.
    private String performedBy;      // userId or system

    private String description;

    @CreatedDate
    private LocalDateTime timestamp;
}