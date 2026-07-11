package com.premisave.property.dto.response;

import com.premisave.property.enums.NoticeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeResponse {
    private String id;
    private String tenantId;
    private String leaseId;
    private String rentalUnitId;
    private NoticeType noticeType;
    private String title;
    private String content;
    private LocalDateTime sentAt;
}