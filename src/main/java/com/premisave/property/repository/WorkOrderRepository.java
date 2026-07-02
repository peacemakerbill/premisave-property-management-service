package com.premisave.property.repository;

import com.premisave.property.entity.WorkOrder;
import com.premisave.property.enums.WorkOrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderRepository extends MongoRepository<WorkOrder, String> {

    List<WorkOrder> findByMaintenanceRequestId(String maintenanceRequestId);

    List<WorkOrder> findByAssignedTo(String assignedTo);

    List<WorkOrder> findByStatus(WorkOrderStatus status);
}