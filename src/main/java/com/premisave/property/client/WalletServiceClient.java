package com.premisave.property.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "wallet-service",
    url = "${wallet-service.url:http://localhost:8084}"
)
public interface WalletServiceClient {

    /**
     * Record rent payment in Wallet Service
     */
    @PostMapping("/internal/transactions/rent")
    void recordRentPayment(@RequestBody Object rentPaymentRequest,
                           @RequestHeader("X-API-Key") String apiKey);

    /**
     * Get wallet balance (example)
     */
    @PostMapping("/internal/wallet/balance")
    Object getWalletBalance(@RequestBody Object request,
                            @RequestHeader("X-API-Key") String apiKey);
}