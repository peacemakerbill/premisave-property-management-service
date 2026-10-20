package com.premisave.property.controller;

import com.premisave.property.dto.request.UpdateWorkOrderStatusRequest;
import com.premisave.property.dto.request.WorkOrderRequest;
import com.premisave.property.dto.response.WorkOrderResponse;
import com.premisave.property.enums.WorkOrderStatus;
import com.premisave.property.exception.ForbiddenException;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final AccessGuard guard;

    // Only whoever manages the unit the request was raised on.
    @PostMapping
    public ResponseEntity<WorkOrderResponse> createWorkOrder(@Valid @RequestBody WorkOrderRequest request) {
        guard.requireMaintenanceManager(request.getMaintenanceRequestId());
        return ResponseEntity.ok(workOrderService.createWorkOrder(request));
    }

    // The assigned technician or the unit's manager.
    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkOrderResponse> updateStatus(@PathVariable String id,
                                                            @Valid @RequestBody UpdateWorkOrderStatusRequest request) {
        guard.requireWorkOrderUpdater(id);
        return ResponseEntity.ok(workOrderService.updateStatus(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> getWorkOrder(@PathVariable String id) {
        guard.requireWorkOrderViewer(id);
        return ResponseEntity.ok(workOrderService.getWorkOrder(id));
    }

    @GetMapping("/maintenance/{maintenanceRequestId}")
    public ResponseEntity<List<WorkOrderResponse>> getWorkOrdersByMaintenanceRequest(
            @PathVariable String maintenanceRequestId) {
        guard.requireMaintenanceViewer(maintenanceRequestId);
        return ResponseEntity.ok(workOrderService.getWorkOrdersByMaintenanceRequest(maintenanceRequestId));
    }

    // A technician's own job list. Admins can look up anyone's.
    @GetMapping("/assignee/{assignedTo}")
    public ResponseEntity<List<WorkOrderResponse>> getWorkOrdersByAssignee(@PathVariable String assignedTo) {
        if (!guard.isAdmin() && !guard.currentUserId().equals(assignedTo)) {
            throw new ForbiddenException("You can only view work orders assigned to you");
        }
        return ResponseEntity.ok(workOrderService.getWorkOrdersByAssignee(assignedTo));
    }

    // Admins see every work order; an owner sees the ones on their own units.
    @GetMapping
    public ResponseEntity<List<WorkOrderResponse>> getWorkOrdersByStatus(
            @RequestParam(defaultValue = "ASSIGNED") WorkOrderStatus status) {
        if (guard.isAdmin()) {
            return ResponseEntity.ok(workOrderService.getWorkOrdersByStatus(status));
        }
        guard.requireOwnerId();
        Set<String> maintenanceIds = guard.ownedMaintenanceIds();
        return ResponseEntity.ok(workOrderService.getWorkOrdersByStatus(status).stream()
                .filter(w -> maintenanceIds.contains(w.getMaintenanceRequestId()))
                .toList());
    }
}
