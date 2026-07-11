package com.premisave.property.enums;

public enum NotificationChannel {
    EMAIL,

    // TODO(SMS-INTEGRATION): not yet wired to a gateway (e.g. Africa's
    // Talking, Twilio). Accepted end-to-end (validated, recorded in
    // NoticeDeliveryResult) but no message is actually sent yet — see
    // SmsService.
    SMS
}