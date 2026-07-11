package com.premisave.property.service;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sends notice SMS via Twilio.
 *
 * Best-effort send — mirrors EmailService's contract exactly: failures are
 * logged, never thrown. The Notice document is already saved by the time
 * this is called, so a Twilio outage (or missing config) must never roll
 * back or block a notice that has already been recorded.
 */
@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    // Numbers on file are frequently local format (e.g. 07XXXXXXXX) rather
    // than E.164, which Twilio requires. Prepended only when the number
    // doesn't already start with '+'.
    @Value("${twilio.default-country-code:+254}")
    private String defaultCountryCode;

    public boolean sendNoticeSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("Skipping notice SMS — recipient has no phone number on file");
            return false;
        }

        if (accountSid == null || accountSid.isBlank() || fromNumber == null || fromNumber.isBlank()) {
            log.warn("Skipping notice SMS to {} — Twilio is not configured "
                    + "(missing TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN or TWILIO_FROM_NUMBER)", phoneNumber);
            return false;
        }

        String to = normalize(phoneNumber);

        try {
            Message sent = Message.creator(
                            new PhoneNumber(to),
                            new PhoneNumber(fromNumber),
                            message)
                    .create();

            log.info("Notice SMS sent to {} (Twilio SID {}, status {})", to, sent.getSid(), sent.getStatus());
            return true;
        } catch (ApiException e) {
            // Twilio-specific errors (bad number, unverified trial number,
            // insufficient funds, etc.) — logged with Twilio's own error code.
            log.error("Twilio rejected notice SMS to {} (code {}): {}", to, e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to send notice SMS to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private String normalize(String phoneNumber) {
        String trimmed = phoneNumber.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        if (trimmed.startsWith("0")) {
            return defaultCountryCode + trimmed.substring(1);
        }
        return defaultCountryCode + trimmed;
    }
}