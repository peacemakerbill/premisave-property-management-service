package com.premisave.property.repository;

import com.premisave.property.entity.ScheduledNotice;
import com.premisave.property.enums.ScheduledNoticeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledNoticeRepository extends MongoRepository<ScheduledNotice, String> {

    // Polled by the scheduler to find jobs that are due for dispatch.
    List<ScheduledNotice> findByStatusAndScheduledAtLessThanEqual(
            ScheduledNoticeStatus status, LocalDateTime time);

    List<ScheduledNotice> findByCreatedByOwnerId(String ownerId);
}