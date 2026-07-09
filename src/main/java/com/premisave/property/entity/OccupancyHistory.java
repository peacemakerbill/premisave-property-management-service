package com.premisave.property.entity;

import com.premisave.property.enums.OccupancyType;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "occupancy_history")
public class OccupancyHistory {

    @Id
    private String id;

    private OccupancyType occupancyType;

    private String rentalUnitId;   // null for WHOLE_PROPERTY occupancy
    private String propertyId;
    private String tenantId;
    private String leaseId;

    private LocalDateTime moveInDate;
    private LocalDateTime moveOutDate;

    @CreatedDate
    private LocalDateTime createdAt;
}