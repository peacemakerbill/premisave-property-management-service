package com.premisave.property.repository;

import com.premisave.property.entity.RentPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RentPaymentRepository extends MongoRepository<RentPayment, String> {

    List<RentPayment> findByLeaseId(String leaseId);

    List<RentPayment> findByTenantId(String tenantId);
}