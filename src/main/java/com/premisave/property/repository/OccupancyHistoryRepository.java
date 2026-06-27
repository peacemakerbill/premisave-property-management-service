package com.premisave.property.repository;

import com.premisave.property.entity.OccupancyHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OccupancyHistoryRepository extends MongoRepository<OccupancyHistory, String> {
}