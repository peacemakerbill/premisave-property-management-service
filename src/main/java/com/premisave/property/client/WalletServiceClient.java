package com.premisave.property.client;

import com.premisave.property.config.WalletServiceFeignConfig;
import com.premisave.property.dto.request.RecordRentPaymentRequest;
import com.premisave.property.dto.response.WalletBalanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "wallet-service",
    url = "${wallet-service.url:http://localhost:8084}",
    configuration = WalletServiceFeignConfig.class
)
public interface WalletServiceClient {

    /**
     * Records a rent payment against the tenant's transaction history in
     * Wallet Service. API key is injected automatically by WalletServiceFeignConfig.
     */
    @PostMapping("/internal/transactions/rent")
    void recordRentPayment(@RequestBody RecordRentPaymentRequest request);

    /**
     * Fetches a user's wallet balance by userId.
     */
    @GetMapping("/internal/wallet/{userId}/balance")
    WalletBalanceResponse getWalletBalance(@PathVariable("userId") String userId);
}