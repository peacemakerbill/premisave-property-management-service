package com.premisave.property.repository;

import com.premisave.property.entity.Owner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerRepository extends MongoRepository<Owner, String> {

    // Linked to Auth userId
}