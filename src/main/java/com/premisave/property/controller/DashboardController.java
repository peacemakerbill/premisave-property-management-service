package com.premisave.property.controller;

import com.premisave.property.dto.response.DashboardSummaryResponse;
import com.premisave.property.service.DashboardService;
import com.premisave.property.service.OwnerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final OwnerService ownerService;

    @GetMapping("/owner")
    public ResponseEntity<DashboardSummaryResponse> getOwnerDashboard(HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        DashboardSummaryResponse summary = dashboardService.getOwnerDashboard(ownerId);
        return ResponseEntity.ok(summary);
    }

    // Same resolution as ReportController — dashboard queries filter by the
    // Owner document's own id (see PropertyRepository.findByOwnerId), not
    // the raw JWT userId, so this has to go through OwnerService first.
    private String resolveOwnerId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ownerService.getOwnerByUserId(userId).getId();
    }
}