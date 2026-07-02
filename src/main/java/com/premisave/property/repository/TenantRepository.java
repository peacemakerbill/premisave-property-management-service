package com.premisave.property.repository;

import com.premisave.property.entity.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends MongoRepository<Tenant, String> {

    Optional<Tenant> findByUserId(String userId);

    Optional<Tenant> findByIdNumber(String idNumber);

    Optional<Tenant> findByEmail(String email);
}