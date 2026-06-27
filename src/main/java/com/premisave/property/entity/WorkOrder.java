package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "work_orders")
public class WorkOrder {

    @Id
    private String id;

    private String maintenanceRequestId;
    private String assignedTo;           // Technician / Vendor ID

    private String title;
    private String description;
    private String status;               // ASSIGNED, IN_PROGRESS, COMPLETED

    private LocalDateTime completedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}