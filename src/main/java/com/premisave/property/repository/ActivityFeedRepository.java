package com.premisave.property.repository;

import com.premisave.property.entity.ActivityFeed;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityFeedRepository extends MongoRepository<ActivityFeed, String> {
}