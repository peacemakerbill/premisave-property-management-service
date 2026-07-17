package com.premisave.property.repository;

import com.premisave.property.entity.SecurityDeposit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityDepositRepository extends MongoRepository<SecurityDeposit, String> {

    Optional<SecurityDeposit> findByLeaseId(String leaseId);

    // A tenant can occupy the same unit across multiple, non-overlapping
    // tenancy periods over time (leave, then return months/years later) —
    // so this is no longer a single Optional. Ordered newest-first so the
    // most recent tenancy period is always first in the list.
    List<SecurityDeposit> findByRentalUnitIdAndTenantIdOrderByCreatedAtDesc(String rentalUnitId, String tenantId);

    List<SecurityDeposit> findByTenantId(String tenantId);
}