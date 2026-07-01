package com.premisave.property.repository;

import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.UnitStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalUnitRepository extends MongoRepository<RentalUnit, String> {

    List<RentalUnit> findByPropertyId(String propertyId);
    
    List<RentalUnit> findByStatus(UnitStatus status);
    
    // Fixed: Added this method
    long countByStatus(UnitStatus status);
}