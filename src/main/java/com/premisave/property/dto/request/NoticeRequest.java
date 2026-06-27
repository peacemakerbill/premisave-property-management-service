package com.premisave.property.dto.request;

import com.premisave.property.enums.NoticeType;
import lombok.Data;

@Data
public class NoticeRequest {
    private String tenantId;
    private NoticeType noticeType;
    private String title;
    private String content;
}