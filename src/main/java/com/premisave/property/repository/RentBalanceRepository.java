package com.premisave.property.repository;

import com.premisave.property.entity.RentBalance;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RentBalanceRepository extends MongoRepository<RentBalance, String> {

    Optional<RentBalance> findByRentalUnitIdAndTenantId(String rentalUnitId, String tenantId);

    List<RentBalance> findByTenantId(String tenantId);
}