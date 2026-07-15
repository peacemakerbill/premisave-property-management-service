package com.premisave.property.repository;

import com.premisave.property.entity.LeaseRentPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LeaseRentPaymentRepository extends MongoRepository<LeaseRentPayment, String> {

    List<LeaseRentPayment> findByLeaseId(String leaseId);

    List<LeaseRentPayment> findByTenantId(String tenantId);

    List<LeaseRentPayment> findByLeaseIdInAndPaidAtBetween(
            List<String> leaseIds, LocalDateTime start, LocalDateTime end);
}