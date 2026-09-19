package com.premisave.property.repository;

import com.premisave.property.entity.LeaseRentUnitPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeaseRentUnitPaymentRepository extends MongoRepository<LeaseRentUnitPayment, String> {

    List<LeaseRentUnitPayment> findByLeaseId(String leaseId);

    List<LeaseRentUnitPayment> findByTenantId(String tenantId);

    List<LeaseRentUnitPayment> findByLeaseIdInAndPaidAtBetween(
            List<String> leaseIds, LocalDateTime start, LocalDateTime end);

    // Idempotent-replay lookup (wallet transfer reference).
    Optional<LeaseRentUnitPayment> findByPaymentReference(String paymentReference);
}