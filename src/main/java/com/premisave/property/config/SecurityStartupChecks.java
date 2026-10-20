package com.premisave.property.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refuses to start without the two secrets the service's security depends on.
 *
 * A blank JWT_SECRET does not just disable signing, it makes JwtService build its key from
 * zero bytes, which anyone can use to forge a token. A blank INTERNAL_API_KEY makes every
 * call to wallet-service and auth-service fail with 401 only after a payment has started.
 * Better to stop at boot with a clear message than to run in either state.
 */
@Slf4j
@Component
class SecurityStartupChecks {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${app.api-key:}")
    private String internalApiKey;

    @PostConstruct
    void verify() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Set it to the same value auth-service signs tokens with.");
        }
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY is not set. Set it to the key shared with auth-service and wallet-service.");
        }
    }
}
