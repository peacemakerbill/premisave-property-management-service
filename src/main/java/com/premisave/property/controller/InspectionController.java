package com.premisave.property.controller;

import com.premisave.property.dto.request.InspectionRequest;
import com.premisave.property.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping
    public ResponseEntity<String> createInspection(@RequestBody InspectionRequest request) {
        inspectionService.createInspection(request);
        return ResponseEntity.ok("Inspection scheduled successfully");
    }
}