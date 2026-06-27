package com.premisave.property.repository;

import com.premisave.property.entity.Property;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends MongoRepository<Property, String> {

    List<Property> findByOwnerId(String ownerId);
    List<Property> findByIsActiveTrue();
    Optional<Property> findByIdAndOwnerId(String id, String ownerId);
}