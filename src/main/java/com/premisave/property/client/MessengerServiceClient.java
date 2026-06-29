package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "messenger-service", url = "${messenger-service.url:}")
public interface MessengerServiceClient {

    @PostMapping("/internal/notifications")
    void sendNotification(@RequestBody Object notificationRequest,
                          @RequestHeader("X-API-Key") String apiKey);
}