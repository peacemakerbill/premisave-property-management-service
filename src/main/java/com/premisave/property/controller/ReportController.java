package com.premisave.property.controller;

import com.premisave.property.dto.response.OccupancyReportResponse;
import com.premisave.property.dto.response.RevenueReportResponse;
import com.premisave.property.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/owner/occupancy")
    public ResponseEntity<OccupancyReportResponse> getOccupancyReport() {
        // ownerId from SecurityContext
        return ResponseEntity.ok(reportService.getOccupancyReport("current-owner-id"));
    }

    @GetMapping("/owner/revenue")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        // ownerId from SecurityContext
        return ResponseEntity.ok(reportService.getRevenueReport("current-owner-id", start, end));
    }
}