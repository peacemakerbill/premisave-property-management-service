package com.premisave.property.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

// Deliberately NOT annotated with @Configuration. Feign client-specific config
// classes must stay outside the main component scan — otherwise this interceptor
// attaches to EVERY Feign client. That matters now: AuthServiceClient.getUserById
// already sends its own X-API-Key, and a second, global X-API-Key header would
// be appended to it.
public class WalletServiceFeignConfig {

    @Value("${app.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor walletServiceApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header("X-API-Key", internalApiKey);
    }

    // Fail fast on connect; allow a realistic read window for a money movement.
    @Bean
    public Request.Options walletServiceRequestOptions() {
        return new Request.Options(5, TimeUnit.SECONDS, 20, TimeUnit.SECONDS, true);
    }

    // Money movements must never be retried implicitly by the HTTP client —
    // retries are decided by WalletPaymentService using the idempotency reference.
    @Bean
    public Retryer walletServiceRetryer() {
        return Retryer.NEVER_RETRY;
    }
}