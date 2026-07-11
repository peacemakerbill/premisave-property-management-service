package com.premisave.property.entity;

import com.premisave.property.enums.RecipientType;
import lombok.Data;

/**
 * Embedded (not a top-level @Document) — stored as a list inside
 * ScheduledNotice so a batch job's full outcome is readable in one fetch.
 */
@Data
public class NoticeDeliveryResult {

    private RecipientType recipientType;
    private String recipientId;    // rentalUnitId or leaseId, depending on recipientType

    private String tenantId;
    private String noticeId;       // set only on success — the Notice document that was created

    private boolean success;
    private String errorMessage;   // set only on failure (e.g. duplicate within 24h, DRAFT lease, vacant unit)

    private boolean emailSent;
    private boolean smsRequested;  // true if SMS was selected — actual sending is not yet implemented
}