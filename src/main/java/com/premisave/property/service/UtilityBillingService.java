package com.premisave.property.service;

import com.premisave.property.dto.request.UtilityBillRequest;
import com.premisave.property.entity.UtilityBill;
import com.premisave.property.repository.UtilityBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UtilityBillingService {

    private final UtilityBillRepository utilityBillRepository;

    public void generateBill(UtilityBillRequest request) {
        UtilityBill bill = new UtilityBill();
        bill.setRentalUnitId(request.getRentalUnitId());
        bill.setUtilityType(request.getUtilityType());
        bill.setAmount(request.getAmount());
        utilityBillRepository.save(bill);
    }
}