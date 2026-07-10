package com.premisave.property.entity;

import com.premisave.property.enums.InspectionStatus;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "inspections")
public class Inspection {

    @Id
    private String id;

    private String rentalUnitId;

    // Who scheduled this — always the home owner, always authenticated
    private String createdByUserId;

    // Who is assigned to carry it out — may or may not have a system account
    private String inspectorFullName;
    private String inspectorPhoneNumber;
    private String inspectorIdNumber;
    private String inspectorEmail;
    private String inspectorUserId;   // set only if the assigned inspector has their own account

    private String title;
    private LocalDate scheduledDate;

    private InspectionStatus status = InspectionStatus.SCHEDULED;

    private String findings;
    private String recommendations;
    private LocalDateTime completedAt;
    private String completedByUserId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}