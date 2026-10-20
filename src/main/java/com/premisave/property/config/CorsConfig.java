package com.premisave.property.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    // Comma separated, set with CORS_ALLOWED_ORIGINS. A single "*" opens the API to every site and
    // is only meant for local experiments, so credentials are switched off when it is used.
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowCredentials(false);
        } else {
            config.setAllowedOrigins(allowedOrigins.stream().map(String::trim).toList());
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Without this a browser hides Retry-After from the frontend, which the 503 "service
        // offline" answers use to tell it when to try again.
        config.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
