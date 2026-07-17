package com.premisave.property.repository;

import com.premisave.property.entity.SecurityDeposit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityDepositRepository extends MongoRepository<SecurityDeposit, String> {

    Optional<SecurityDeposit> findByLeaseId(String leaseId);

    Optional<SecurityDeposit> findByRentalUnitIdAndTenantId(String rentalUnitId, String tenantId);

    List<SecurityDeposit> findByTenantId(String tenantId);
}