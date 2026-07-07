package com.premisave.property.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Deliberately NOT annotated with @Configuration. Feign client-specific config
// classes must stay outside the main component scan — otherwise this interceptor
// would attach to every Feign client (AuthServiceClient, BookingServiceClient,
// ListingServiceClient) instead of just WalletServiceClient.
@Configuration
public class WalletServiceFeignConfig {

    @Value("${app.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor walletServiceApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header("X-API-Key", internalApiKey);
    }
}