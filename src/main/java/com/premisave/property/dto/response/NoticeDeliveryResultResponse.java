package com.premisave.property.dto.response;

import com.premisave.property.enums.RecipientType;
import lombok.Data;

@Data
public class NoticeDeliveryResultResponse {
    private RecipientType recipientType;
    private String recipientId;
    private String tenantId;
    private String noticeId;
    private boolean success;
    private String errorMessage;
    private boolean emailSent;
    private boolean smsRequested;
    private boolean smsSent;
}