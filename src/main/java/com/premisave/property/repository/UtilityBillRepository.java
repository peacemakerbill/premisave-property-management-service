package com.premisave.property.repository;

import com.premisave.property.entity.UtilityBill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilityBillRepository extends MongoRepository<UtilityBill, String> {
}