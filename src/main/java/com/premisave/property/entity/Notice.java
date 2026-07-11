package com.premisave.property.entity;

import com.premisave.property.enums.NoticeType;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "notices")
public class Notice {

    @Id
    private String id;

    private String tenantId;
    private String leaseId;        // set for lease-backed notices, null for direct-unit notices
    private String rentalUnitId;   // set for unit-based notices; null for whole-property lease notices
    private String propertyId;     // always set (resolved from unit or lease) so reads can show property context

    private NoticeType noticeType;
    private String title;
    private String content;

    private LocalDateTime sentAt;

    @CreatedDate
    private LocalDateTime createdAt;
}