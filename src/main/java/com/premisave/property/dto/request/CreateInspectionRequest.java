package com.premisave.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateInspectionRequest {

    @NotBlank
    private String rentalUnitId;

    @NotBlank
    private String title;

    @NotNull
    private LocalDate scheduledDate;

    // Provide these to assign an inspector with no system account.
    // Omit all of them to leave the inspection unassigned for now
    // (assignment can happen later, or the home owner completes it themselves).
    private String inspectorFullName;
    private String inspectorPhoneNumber;
    private String inspectorIdNumber;
    private String inspectorEmail;
}