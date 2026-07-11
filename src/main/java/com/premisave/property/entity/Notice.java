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
    private String rentalUnitId;   // set for direct-unit notices and lease-backed unit leases; null for whole-property leases

    private NoticeType noticeType;
    private String title;
    private String content;

    private LocalDateTime sentAt;

    @CreatedDate
    private LocalDateTime createdAt;
}