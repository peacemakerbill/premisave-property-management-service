package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service", url = "${payment-service.url:}")
public interface PaymentServiceClient {

    @PostMapping("/internal/payments/process")
    Object processPayment(@RequestBody Object paymentRequest,
                          @RequestHeader("X-API-Key") String apiKey);
}