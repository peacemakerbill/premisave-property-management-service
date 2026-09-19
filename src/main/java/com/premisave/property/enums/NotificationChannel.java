package com.premisave.property.enums;

public enum NotificationChannel {
    EMAIL,

    // Not supported: there is no SMS gateway. The value stays so that a request asking
    // for SMS gets a clear 400 from NoticeSchedulingService instead of a JSON parse error.
    SMS
}