package com.premisave.property.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Twilio SDK once at application startup. Deliberately
 * tolerant of missing credentials — if TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN
 * aren't set, we log a warning instead of failing the whole application
 * context, since SMS is a best-effort notice-delivery channel (mirrors how
 * EmailService/SmsService never let a notification-channel outage take down
 * a request that already recorded a Notice).
 */
@Slf4j
@Configuration
public class TwilioConfig {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @PostConstruct
    public void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.warn("Twilio account SID/auth token not configured — SMS notices will be skipped until "
                    + "TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN are set");
            return;
        }
        Twilio.init(accountSid, authToken);
        log.info("Twilio client initialized");
    }
}