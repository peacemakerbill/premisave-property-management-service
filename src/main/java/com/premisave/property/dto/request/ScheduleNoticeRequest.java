package com.premisave.property.dto.request;

import com.premisave.property.enums.NoticeType;
import com.premisave.property.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleNoticeRequest {

    // Direct (non-lease) unit recipients — current occupant of each unit
    // is resolved automatically. Provide at least one of rentalUnitIds or
    // leaseIds (both may be used together in the same batch).
    private List<String> rentalUnitIds;

    // Lease-backed recipients.
    private List<String> leaseIds;

    @NotNull
    private NoticeType noticeType;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotEmpty(message = "At least one delivery channel is required")
    private List<NotificationChannel> channels;

    // Omit, or set to now/a past time, to send immediately.
    // A future timestamp queues the notice for the scheduler to dispatch.
    private LocalDateTime scheduledAt;
}