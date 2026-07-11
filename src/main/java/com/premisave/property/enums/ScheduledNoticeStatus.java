package com.premisave.property.enums;

public enum ScheduledNoticeStatus {
    PENDING,          // scheduled for the future, not yet processed
    PROCESSING,        // currently being dispatched (instant send, or picked up by the scheduler)
    SENT,              // all recipients processed successfully
    PARTIALLY_SENT,    // some recipients succeeded, some failed
    FAILED,            // all recipients failed, or the job itself errored out
    CANCELLED          // cancelled by the owner before it was processed
}