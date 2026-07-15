package com.premisave.property.repository;

import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.enums.OccupancyType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OccupancyHistoryRepository extends MongoRepository<OccupancyHistory, String> {

    List<OccupancyHistory> findByRentalUnitId(String rentalUnitId);

    List<OccupancyHistory> findByPropertyId(String propertyId);

    List<OccupancyHistory> findByTenantId(String tenantId);

    Optional<OccupancyHistory> findByRentalUnitIdAndTenantIdAndMoveOutDateIsNull(
            String rentalUnitId, String tenantId);

    Optional<OccupancyHistory> findByRentalUnitIdAndMoveOutDateIsNull(String rentalUnitId);

    Optional<OccupancyHistory> findByLeaseIdAndMoveOutDateIsNull(String leaseId);

    Optional<OccupancyHistory> findByPropertyIdAndOccupancyTypeAndMoveOutDateIsNull(
            String propertyId, OccupancyType occupancyType);

    // All currently-active direct (no-lease) occupancies — used by
    // RentBalanceService's monthly charge job so arrears accrue even if
    // the tenant hasn't made a payment recently.
    List<OccupancyHistory> findByLeaseIdIsNullAndMoveOutDateIsNull();
}