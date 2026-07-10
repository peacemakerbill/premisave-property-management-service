package com.premisave.property.repository;

import com.premisave.property.entity.Maintenance;
import com.premisave.property.enums.MaintenanceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends MongoRepository<Maintenance, String> {

    List<Maintenance> findByTenantId(String tenantId);

    List<Maintenance> findByRentalUnitId(String rentalUnitId);

    List<Maintenance> findByStatus(MaintenanceStatus status);
    
    List<Maintenance> findByRentalUnitIdInAndStatusIn(List<String> rentalUnitIds, List<MaintenanceStatus> statuses);

    // Used to block duplicate submissions of the same complaint by the same
    // tenant, for the same unit, within a rolling time window.
    List<Maintenance> findByTenantIdAndRentalUnitIdAndTitleIgnoreCaseAndCreatedAtAfter(
            String tenantId, String rentalUnitId, String title, LocalDateTime since);
}