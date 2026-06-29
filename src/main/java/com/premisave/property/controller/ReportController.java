package com.premisave.property.controller;

import com.premisave.property.dto.response.DashboardSummaryResponse;
import com.premisave.property.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/owner/summary")
    public ResponseEntity<DashboardSummaryResponse> getReportSummary() {
        // Call report service
        return ResponseEntity.ok(new DashboardSummaryResponse());
    }
}