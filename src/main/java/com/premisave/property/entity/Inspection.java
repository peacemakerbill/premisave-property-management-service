package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "inspections")
public class Inspection {

    @Id
    private String id;

    private String rentalUnitId;
    private String inspectorId;

    private String title;
    private String findings;
    private String recommendations;

    private LocalDateTime inspectionDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}