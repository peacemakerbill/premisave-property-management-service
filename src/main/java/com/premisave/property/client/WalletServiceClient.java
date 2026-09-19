package com.premisave.property.client;

import com.premisave.property.config.WalletServiceFeignConfig;
import com.premisave.property.dto.wallet.WalletApiResponse;
import com.premisave.property.dto.wallet.WalletTransactionResult;
import com.premisave.property.dto.wallet.WalletTransferRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Only calls endpoints that actually exist in wallet-service. (The previous
 * /internal/transactions/rent and /internal/wallet/{userId}/balance do not.)
 * The X-API-Key header is added by WalletServiceFeignConfig.
 */
@FeignClient(
    name = "wallet-service",
    url = "${wallet-service.url:http://localhost:8084}",
    configuration = WalletServiceFeignConfig.class
)
public interface WalletServiceClient {

    /** Wallet-to-wallet transfer, sender supplied explicitly (service-to-service). */
    @PostMapping("/internal/transfer")
    WalletApiResponse<WalletTransactionResult> transfer(@RequestBody WalletTransferRequest request);
}