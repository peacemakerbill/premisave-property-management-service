package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service", url = "${notification-service.url:}")
public interface NotificationServiceClient {

    @PostMapping("/send/email")
    void sendEmail(@RequestBody Object emailRequest,
                   @RequestHeader("X-API-Key") String apiKey);

    @PostMapping("/send/sms")
    void sendSms(@RequestBody Object smsRequest,
                 @RequestHeader("X-API-Key") String apiKey);
}