package com.premisave.property.service;

import com.premisave.property.dto.request.UpdateWorkOrderStatusRequest;
import com.premisave.property.dto.request.WorkOrderRequest;
import com.premisave.property.dto.response.WorkOrderResponse;
import com.premisave.property.entity.Maintenance;
import com.premisave.property.entity.WorkOrder;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.enums.WorkOrderStatus;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.MaintenanceRequestRepository;
import com.premisave.property.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final MaintenanceRequestRepository maintenanceRepository;

    @Transactional
    public WorkOrderResponse createWorkOrder(WorkOrderRequest request) {
        Maintenance maintenance = maintenanceRepository.findById(request.getMaintenanceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));

        WorkOrder workOrder = new WorkOrder();
        workOrder.setMaintenanceRequestId(request.getMaintenanceRequestId());
        workOrder.setAssignedTo(request.getAssignedTo());
        workOrder.setTitle(request.getTitle());
        workOrder.setDescription(request.getDescription());
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);

        WorkOrder saved = workOrderRepository.save(workOrder);

        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenanceRepository.save(maintenance);

        return toResponse(saved);
    }

    @Transactional
    public WorkOrderResponse updateStatus(String id, UpdateWorkOrderStatusRequest request) {
        WorkOrder workOrder = findOrThrow(id);
        workOrder.setStatus(request.getStatus());

        if (request.getStatus() == WorkOrderStatus.COMPLETED) {
            workOrder.setCompletedAt(LocalDateTime.now());
        }

        WorkOrder saved = workOrderRepository.save(workOrder);

        syncMaintenanceStatus(workOrder.getMaintenanceRequestId(), request.getStatus());

        return toResponse(saved);
    }

    public WorkOrderResponse getWorkOrder(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<WorkOrderResponse> getWorkOrdersByMaintenanceRequest(String maintenanceRequestId) {
        return workOrderRepository.findByMaintenanceRequestId(maintenanceRequestId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WorkOrderResponse> getWorkOrdersByAssignee(String assignedTo) {
        return workOrderRepository.findByAssignedTo(assignedTo).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WorkOrderResponse> getWorkOrdersByStatus(WorkOrderStatus status) {
        return workOrderRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    private void syncMaintenanceStatus(String maintenanceRequestId, WorkOrderStatus workOrderStatus) {
        maintenanceRepository.findById(maintenanceRequestId).ifPresent(maintenance -> {
            MaintenanceStatus mapped = switch (workOrderStatus) {
                case ASSIGNED, IN_PROGRESS -> MaintenanceStatus.IN_PROGRESS;
                case COMPLETED -> MaintenanceStatus.COMPLETED;
                case CANCELLED -> MaintenanceStatus.CANCELLED;
            };
            maintenance.setStatus(mapped);
            maintenanceRepository.save(maintenance);
        });
    }

    private WorkOrder findOrThrow(String id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found"));
    }

    private WorkOrderResponse toResponse(WorkOrder workOrder) {
        WorkOrderResponse response = new WorkOrderResponse();
        response.setId(workOrder.getId());
        response.setMaintenanceRequestId(workOrder.getMaintenanceRequestId());
        response.setAssignedTo(workOrder.getAssignedTo());
        response.setTitle(workOrder.getTitle());
        response.setDescription(workOrder.getDescription());
        response.setStatus(workOrder.getStatus());
        response.setCompletedAt(workOrder.getCompletedAt());
        return response;
    }
}