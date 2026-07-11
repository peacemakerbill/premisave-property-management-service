package com.premisave.property.dto.request;

import com.premisave.property.enums.NoticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnitNoticeRequest {

    @NotBlank
    private String rentalUnitId;

    // Optional — if provided, must match the unit's current occupant.
    // If omitted, the current occupant is resolved automatically from
    // the unit's active (non-lease) occupancy record.
    private String tenantId;

    @NotNull
    private NoticeType noticeType;

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}