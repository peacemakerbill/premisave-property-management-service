package com.premisave.property.entity;

import com.premisave.property.enums.NoticeType;
import com.premisave.property.enums.NotificationChannel;
import com.premisave.property.enums.ScheduledNoticeStatus;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "scheduled_notices")
public class ScheduledNotice {

    @Id
    private String id;

    private String createdByOwnerId;

    private List<String> rentalUnitIds;   // direct (non-lease) unit recipients
    private List<String> leaseIds;        // lease-backed recipients

    private NoticeType noticeType;
    private String title;
    private String content;
    private List<NotificationChannel> channels;

    // null => processed immediately at creation time.
    // A future timestamp means the scheduler poller picks it up when due.
    private LocalDateTime scheduledAt;

    private ScheduledNoticeStatus status = ScheduledNoticeStatus.PENDING;

    private List<NoticeDeliveryResult> results;
    private LocalDateTime processedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}