package com.premisave.property.repository;

import com.premisave.property.entity.WorkOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkOrderRepository extends MongoRepository<WorkOrder, String> {
}