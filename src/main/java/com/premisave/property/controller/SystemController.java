package com.premisave.property.controller;

import com.premisave.property.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final JwtService jwtService;

    // Off unless SYSTEM_TEST_TOKEN_ENABLED=true. It decodes a token and shows its claims, which is
    // handy while wiring up the frontend and not something a production service should offer.
    @Value("${system.test-token-enabled:false}")
    private boolean testTokenEnabled;

    // ====================== HEALTH CHECKS ======================

    // Public, for load balancers and the other services' availability probes: status only.
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", "Premisave Property Management Service");
        status.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(status);
    }

    // Runtime details for operators, so it needs an admin token.
    @GetMapping("/health/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> healthDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");
        details.put("service", "Premisave Property Management Service");
        details.put("version", "0.0.1-SNAPSHOT");
        details.put("environment", System.getProperty("spring.profiles.active", "default"));
        details.put("javaVersion", System.getProperty("java.version"));
        details.put("database", "MongoDB");
        details.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(details);
    }

    // ====================== TOKEN TEST ENDPOINT ======================
    @GetMapping("/test-token")
    public ResponseEntity<Map<String, Object>> testToken(@RequestHeader("Authorization") String authorization) {
        if (!testTokenEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "FAILURE");
            error.put("message", "Missing or malformed Authorization header. Expected: Bearer <token>");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String token = authorization.substring(7);

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            boolean valid = jwtService.isTokenValid(token);
            response.put("status", valid ? "SUCCESS" : "FAILURE");
            response.put("message", valid ? "JWT Token is valid and working" : "Token is expired");
            response.put("email", jwtService.extractEmail(token));
            response.put("userId", jwtService.extractUserId(token));
            response.put("role", jwtService.extractRole(token));
            response.put("timestamp", LocalDateTime.now());
            response.put("authorizationHeaderPresent", true);

            return valid ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("status", "FAILURE");
            response.put("message", "Token could not be parsed or verified");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
