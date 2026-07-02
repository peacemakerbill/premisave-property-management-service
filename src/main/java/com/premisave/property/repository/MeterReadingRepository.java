package com.premisave.property.repository;

import com.premisave.property.entity.MeterReading;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends MongoRepository<MeterReading, String> {

    List<MeterReading> findByRentalUnitId(String rentalUnitId);

    List<MeterReading> findByRentalUnitIdAndMeterType(String rentalUnitId, String meterType);

    Optional<MeterReading> findFirstByRentalUnitIdAndMeterTypeOrderByReadingDateDesc(
            String rentalUnitId, String meterType);
}