package com.premisave.property.repository;

import com.premisave.property.entity.RentSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RentScheduleRepository extends MongoRepository<RentSchedule, String> {

    List<RentSchedule> findByLeaseId(String leaseId);
    
    List<RentSchedule> findByDueDateBetween(LocalDate start, LocalDate end);
}