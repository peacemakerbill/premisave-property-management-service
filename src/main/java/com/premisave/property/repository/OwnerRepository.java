package com.premisave.property.repository;

import com.premisave.property.entity.Owner;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OwnerRepository extends MongoRepository<Owner, String> {
    Optional<Owner> findByUserId(String userId);
}