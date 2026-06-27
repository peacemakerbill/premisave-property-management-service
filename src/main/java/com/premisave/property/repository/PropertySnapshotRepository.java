package com.premisave.property.repository;

import com.premisave.property.entity.PropertySnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertySnapshotRepository extends MongoRepository<PropertySnapshot, String> {

}