package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "property_snapshots")
public class PropertySnapshot {

    @Id
    private String id;

    private String propertyId;
    private String snapshotData;   // JSON or serialized state

    private LocalDateTime snapshotDate;
}