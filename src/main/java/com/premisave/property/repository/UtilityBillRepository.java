package com.premisave.property.repository;

import com.premisave.property.entity.UtilityBill;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.UtilityType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UtilityBillRepository extends MongoRepository<UtilityBill, String> {

    List<UtilityBill> findByRentalUnitId(String rentalUnitId);

    List<UtilityBill> findByTenantId(String tenantId);

    List<UtilityBill> findByTenantIdAndStatusIn(String tenantId, List<PaymentStatus> statuses);

    // --- new, for duplicate-bill prevention ---
    boolean existsBySourceMeterReadingId(String sourceMeterReadingId);

    List<UtilityBill> findByRentalUnitIdAndUtilityType(String rentalUnitId, UtilityType utilityType);
}