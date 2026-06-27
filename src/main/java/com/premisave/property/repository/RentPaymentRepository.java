package com.premisave.property.repository;

import com.premisave.property.entity.RentPayment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RentPaymentRepository extends MongoRepository<RentPayment, String> {
}