package com.premisave.property.service;

import com.premisave.property.dto.request.WorkOrderRequest;
import com.premisave.property.entity.WorkOrder;
import com.premisave.property.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;

    @Transactional
    public void createWorkOrder(WorkOrderRequest request) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setMaintenanceRequestId(request.getMaintenanceRequestId());
        workOrder.setAssignedTo(request.getAssignedTo());
        workOrder.setTitle(request.getTitle());
        workOrder.setDescription(request.getDescription());
        workOrder.setStatus("ASSIGNED");
        workOrderRepository.save(workOrder);
    }
}