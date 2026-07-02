package com.premisave.property.repository;

import com.premisave.property.entity.RentSchedule;
import com.premisave.property.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RentScheduleRepository extends MongoRepository<RentSchedule, String> {

    List<RentSchedule> findByLeaseId(String leaseId);

    Optional<RentSchedule> findFirstByLeaseIdAndStatusInOrderByDueDateAsc(String leaseId, List<PaymentStatus> statuses);

    List<RentSchedule> findByDueDateBeforeAndStatusIn(LocalDate date, List<PaymentStatus> statuses);
}