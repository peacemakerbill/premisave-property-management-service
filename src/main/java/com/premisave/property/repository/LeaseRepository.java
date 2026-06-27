package com.premisave.property.repository;

import com.premisave.property.entity.Lease;
import com.premisave.property.enums.LeaseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaseRepository extends MongoRepository<Lease, String> {

    List<Lease> findByTenantId(String tenantId);
    List<Lease> findByStatus(LeaseStatus status);
}