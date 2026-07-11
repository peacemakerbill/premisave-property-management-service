package com.premisave.property.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TODO(SMS-INTEGRATION): wire up an SMS gateway here (e.g. Africa's Talking,
 * Twilio). This stub exists so NotificationChannel.SMS can be selected
 * end-to-end today — requests are validated and recorded in
 * NoticeDeliveryResult.smsRequested — without breaking the notice-sending
 * flow. No message is actually sent yet.
 */
@Slf4j
@Service
public class SmsService {

    public boolean sendNoticeSms(String phoneNumber, String message) {
        log.warn("SMS requested for {} but SMS sending is not yet implemented (TODO)", phoneNumber);
        return false;
    }
}