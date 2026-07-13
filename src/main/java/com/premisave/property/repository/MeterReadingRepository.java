package com.premisave.property.repository;

import com.premisave.property.entity.MeterReading;
import com.premisave.property.enums.MeterType;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends MongoRepository<MeterReading, String> {

    List<MeterReading> findByRentalUnitId(String rentalUnitId);

    Optional<MeterReading> findFirstByRentalUnitIdAndMeterTypeOrderByReadingDateDesc(
            String rentalUnitId, MeterType meterType);

    boolean existsByRentalUnitIdAndMeterTypeAndReadingDateBetween(
            String rentalUnitId, MeterType meterType, LocalDateTime start, LocalDateTime end);
}