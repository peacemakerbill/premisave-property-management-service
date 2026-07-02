package com.premisave.property.repository;

import com.premisave.property.entity.RentPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RentPaymentRepository extends MongoRepository<RentPayment, String> {

    List<RentPayment> findByLeaseId(String leaseId);

    List<RentPayment> findByTenantId(String tenantId);

    List<RentPayment> findByPropertyIdInAndPaidAtBetween(
            List<String> propertyIds, LocalDateTime start, LocalDateTime end);
}