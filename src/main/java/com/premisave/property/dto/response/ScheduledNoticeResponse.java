package com.premisave.property.dto.response;

import com.premisave.property.enums.NoticeType;
import com.premisave.property.enums.NotificationChannel;
import com.premisave.property.enums.ScheduledNoticeStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduledNoticeResponse {
    private String id;
    private ScheduledNoticeStatus status;
    private NoticeType noticeType;
    private String title;
    private List<NotificationChannel> channels;
    private LocalDateTime scheduledAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    private int totalRecipients;
    private int successCount;
    private int failureCount;

    private List<NoticeDeliveryResultResponse> results;
}