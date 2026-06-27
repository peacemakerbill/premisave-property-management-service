package com.premisave.property.repository;

import com.premisave.property.entity.SecurityDeposit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityDepositRepository extends MongoRepository<SecurityDeposit, String> {
}