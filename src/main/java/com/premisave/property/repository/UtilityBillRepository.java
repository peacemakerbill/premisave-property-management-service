package com.premisave.property.repository;

import com.premisave.property.entity.UtilityBill;
import com.premisave.property.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtilityBillRepository extends MongoRepository<UtilityBill, String> {

    List<UtilityBill> findByRentalUnitId(String rentalUnitId);

    List<UtilityBill> findByTenantId(String tenantId);

    List<UtilityBill> findByTenantIdAndStatusIn(String tenantId, List<PaymentStatus> statuses);
}