package com.premisave.property.repository;

import com.premisave.property.entity.OccupancyHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OccupancyHistoryRepository extends MongoRepository<OccupancyHistory, String> {

    List<OccupancyHistory> findByRentalUnitId(String rentalUnitId);

    List<OccupancyHistory> findByTenantId(String tenantId);

    Optional<OccupancyHistory> findByRentalUnitIdAndTenantIdAndMoveOutDateIsNull(
            String rentalUnitId, String tenantId);

    Optional<OccupancyHistory> findByRentalUnitIdAndMoveOutDateIsNull(String rentalUnitId);
}