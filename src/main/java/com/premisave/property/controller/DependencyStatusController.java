package com.premisave.property.controller;

import com.premisave.property.health.ExternalService;
import com.premisave.property.health.ServiceHealthMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets the frontend ask "are the services behind payments and profile sync up?" BEFORE the
 * user tries — e.g. to disable a Pay button and show a banner. Requires a valid JWT.
 */
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class DependencyStatusController {

    private final ServiceHealthMonitor healthMonitor;

    @GetMapping("/dependencies")
    public ResponseEntity<Map<String, Object>> dependencies() {
        Map<String, Object> services = new LinkedHashMap<>();
        boolean allUp = true;

        for (ExternalService service : ExternalService.values()) {
            boolean online = healthMonitor.isOnline(service);
            allUp &= online;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", service.getDisplayName());
            entry.put("status", online ? "UP" : "DOWN");
            services.put(service.getTargetName(), entry);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", allUp ? "UP" : "DEGRADED");
        body.put("services", services);
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(body);
    }
}