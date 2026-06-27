package com.premisave.property.entity;

import com.premisave.property.enums.MaintenanceStatus;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "maintenance_requests")
public class MaintenanceRequest {

    @Id
    private String id;

    private String tenantId;
    private String rentalUnitId;

    private String title;
    private String description;

    private MaintenanceStatus status = MaintenanceStatus.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}