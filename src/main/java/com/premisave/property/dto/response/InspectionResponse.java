package com.premisave.property.dto.response;

import com.premisave.property.enums.InspectionStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InspectionResponse {
    private String id;
    private String rentalUnitId;
    private RentalUnitSummaryResponse rentalUnit;
    private String createdByUserId;
    private String inspectorFullName;
    private String inspectorPhoneNumber;
    private String inspectorIdNumber;
    private String inspectorEmail;
    private String inspectorUserId;
    private String title;
    private LocalDate scheduledDate;
    private InspectionStatus status;
    private String findings;
    private String recommendations;
    private LocalDateTime completedAt;
    private String completedByUserId;
}