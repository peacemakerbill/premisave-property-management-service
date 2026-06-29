package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "wallet-service", url = "${wallet-service.url}")
public interface WalletServiceClient {

    @PostMapping("/internal/transactions/rent")
    void recordRentPayment(@RequestBody Object paymentRequest,
                           @RequestHeader("X-API-Key") String apiKey);
}