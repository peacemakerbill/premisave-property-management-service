package com.premisave.property.repository;

import com.premisave.property.entity.Maintenance;
import com.premisave.property.enums.MaintenanceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends MongoRepository<Maintenance, String> {

    List<Maintenance> findByTenantId(String tenantId);

    List<Maintenance> findByRentalUnitId(String rentalUnitId);

    List<Maintenance> findByStatus(MaintenanceStatus status);
}