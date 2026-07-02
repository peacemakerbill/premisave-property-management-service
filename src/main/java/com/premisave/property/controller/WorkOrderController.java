package com.premisave.property.controller;

import com.premisave.property.dto.request.UpdateWorkOrderStatusRequest;
import com.premisave.property.dto.request.WorkOrderRequest;
import com.premisave.property.dto.response.WorkOrderResponse;
import com.premisave.property.enums.WorkOrderStatus;
import com.premisave.property.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> createWorkOrder(@Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.createWorkOrder(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkOrderResponse> updateStatus(@PathVariable String id,
                                                            @Valid @RequestBody UpdateWorkOrderStatusRequest request) {
        return ResponseEntity.ok(workOrderService.updateStatus(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> getWorkOrder(@PathVariable String id) {
        return ResponseEntity.ok(workOrderService.getWorkOrder(id));
    }

    @GetMapping("/maintenance/{maintenanceRequestId}")
    public ResponseEntity<List<WorkOrderResponse>> getWorkOrdersByMaintenanceRequest(
            @PathVariable String maintenanceRequestId) {
        return ResponseEntity.ok(workOrderService.getWorkOrdersByMaintenanceRequest(maintenanceRequestId));
    }

    @GetMapping("/assignee/{assignedTo}")
    public ResponseEntity<List<WorkOrderResponse>> getWorkOrdersByAssignee(@PathVariable String assignedTo) {
        return ResponseEntity.ok(workOrderService.getWorkOrdersByAssignee(assignedTo));
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderResponse>> getWorkOrdersByStatus(
            @RequestParam(defaultValue = "ASSIGNED") WorkOrderStatus status) {
        return ResponseEntity.ok(workOrderService.getWorkOrdersByStatus(status));
    }
}