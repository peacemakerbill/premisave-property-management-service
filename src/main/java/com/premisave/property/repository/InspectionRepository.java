package com.premisave.property.repository;

import com.premisave.property.entity.Inspection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionRepository extends MongoRepository<Inspection, String> {
}