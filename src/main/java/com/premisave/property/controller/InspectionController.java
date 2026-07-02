package com.premisave.property.controller;

import com.premisave.property.dto.request.InspectionRequest;
import com.premisave.property.dto.request.UpdateInspectionRequest;
import com.premisave.property.dto.response.InspectionResponse;
import com.premisave.property.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping
    public ResponseEntity<InspectionResponse> createInspection(@Valid @RequestBody InspectionRequest request) {
        // inspectorId from SecurityContext
        return ResponseEntity.ok(inspectionService.createInspection(request, "inspector-id-from-jwt"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> getInspection(@PathVariable String id) {
        return ResponseEntity.ok(inspectionService.getInspection(id));
    }

    @GetMapping("/unit/{rentalUnitId}")
    public ResponseEntity<List<InspectionResponse>> getInspectionsByUnit(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(inspectionService.getInspectionsByUnit(rentalUnitId));
    }

    @GetMapping("/inspector/{inspectorId}")
    public ResponseEntity<List<InspectionResponse>> getInspectionsByInspector(@PathVariable String inspectorId) {
        return ResponseEntity.ok(inspectionService.getInspectionsByInspector(inspectorId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InspectionResponse> updateFindings(@PathVariable String id,
                                                               @RequestBody UpdateInspectionRequest request) {
        return ResponseEntity.ok(inspectionService.updateFindings(id, request));
    }
}