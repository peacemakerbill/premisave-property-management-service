package com.premisave.property.repository;

import com.premisave.property.entity.Inspection;
import com.premisave.property.enums.InspectionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InspectionRepository extends MongoRepository<Inspection, String> {

    List<Inspection> findByRentalUnitId(String rentalUnitId);

    List<Inspection> findByInspectorUserId(String inspectorUserId);

    List<Inspection> findByCreatedByUserId(String createdByUserId);

    List<Inspection> findByCreatedByUserIdAndStatus(String createdByUserId, InspectionStatus status);

    List<Inspection> findByRentalUnitIdAndScheduledDateAndStatusNot(
            String rentalUnitId, LocalDate scheduledDate, InspectionStatus excludedStatus);
}