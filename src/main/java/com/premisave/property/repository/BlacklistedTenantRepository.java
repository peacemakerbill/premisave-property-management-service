package com.premisave.property.repository;

import com.premisave.property.entity.BlacklistedTenant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistedTenantRepository extends MongoRepository<BlacklistedTenant, String> {

    Optional<BlacklistedTenant> findByTenantId(String tenantId);
}