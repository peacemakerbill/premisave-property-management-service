package com.premisave.property.repository;

import com.premisave.property.entity.UnitRentPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UnitRentPaymentRepository extends MongoRepository<UnitRentPayment, String> {

    List<UnitRentPayment> findByRentalUnitId(String rentalUnitId);

    List<UnitRentPayment> findByTenantId(String tenantId);
}