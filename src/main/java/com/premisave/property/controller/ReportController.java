package com.premisave.property.controller;

import com.premisave.property.dto.response.OccupancyReportResponse;
import com.premisave.property.dto.response.RevenueReportResponse;
import com.premisave.property.service.OwnerService;
import com.premisave.property.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OwnerService ownerService;

    @GetMapping("/owner/occupancy")
    public ResponseEntity<OccupancyReportResponse> getOccupancyReport(HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        return ResponseEntity.ok(reportService.getOccupancyReport(ownerId));
    }

    @GetMapping("/owner/revenue")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletRequest httpRequest) {
        String ownerId = resolveOwnerId(httpRequest);
        return ResponseEntity.ok(reportService.getRevenueReport(ownerId, start, end));
    }

    // Reports filter by the Owner document's own id (see
    // PropertyRepository.findByOwnerId), not the raw JWT userId — those are
    // two different values (Owner.userId vs Owner.id). Resolve through
    // OwnerService the same way OwnerController.getMyOwnerProfile() does.
    private String resolveOwnerId(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ownerService.getOwnerByUserId(userId).getId();
    }
}