package com.premisave.property.controller;

import com.premisave.property.dto.response.DashboardSummaryResponse;
import com.premisave.property.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/owner")
    public ResponseEntity<DashboardSummaryResponse> getOwnerDashboard() {
        DashboardSummaryResponse summary = dashboardService.getOwnerDashboard("current-owner-id");
        return ResponseEntity.ok(summary);
    }
}