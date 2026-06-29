package com.premisave.property.controller;

import com.premisave.property.dto.request.WorkOrderRequest;
import com.premisave.property.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<String> createWorkOrder(@RequestBody WorkOrderRequest request) {
        workOrderService.createWorkOrder(request);
        return ResponseEntity.ok("Work order created successfully");
    }
}