package com.premisave.property.controller;

import com.premisave.property.dto.request.CompleteInspectionRequest;
import com.premisave.property.dto.request.CreateInspectionRequest;
import com.premisave.property.dto.response.InspectionResponse;
import com.premisave.property.enums.InspectionStatus;
import com.premisave.property.service.InspectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<InspectionResponse> createInspection(@Valid @RequestBody CreateInspectionRequest request,
                                                                  HttpServletRequest httpRequest) {
        String createdByUserId = (String) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(inspectionService.createInspection(request, createdByUserId));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<InspectionResponse> completeInspection(@PathVariable String id,
                                                                     @Valid @RequestBody CompleteInspectionRequest request,
                                                                     HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(inspectionService.completeInspection(id, request, userId));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<InspectionResponse> cancelInspection(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(inspectionService.cancelInspection(id, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> getInspection(@PathVariable String id) {
        return ResponseEntity.ok(inspectionService.getInspection(id));
    }

    @GetMapping("/unit/{rentalUnitId}")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<List<InspectionResponse>> getInspectionsByUnit(@PathVariable String rentalUnitId) {
        return ResponseEntity.ok(inspectionService.getInspectionsByUnit(rentalUnitId));
    }

    @GetMapping("/inspector/{inspectorUserId}")
    public ResponseEntity<List<InspectionResponse>> getInspectionsByInspector(@PathVariable String inspectorUserId,
                                                                                HttpServletRequest httpRequest) {
        String callerUserId = (String) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(inspectionService.getInspectionsByInspector(inspectorUserId, callerUserId));
    }

    @GetMapping("/created-by/{createdByUserId}")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<List<InspectionResponse>> getInspectionsCreatedBy(@PathVariable String createdByUserId,
                                                                               HttpServletRequest httpRequest) {
        String callerUserId = (String) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(inspectionService.getInspectionsCreatedBy(createdByUserId, callerUserId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('HOME_OWNER')")
    public ResponseEntity<List<InspectionResponse>> getMyInspectionsByStatus(
            @RequestParam InspectionStatus status, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(inspectionService.getMyInspectionsByStatus(userId, status));
    }
}