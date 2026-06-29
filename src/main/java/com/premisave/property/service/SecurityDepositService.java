package com.premisave.property.service;

import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.entity.SecurityDeposit;
import com.premisave.property.repository.SecurityDepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SecurityDepositService {

    private final SecurityDepositRepository depositRepository;

    @Transactional
    public void holdDeposit(SecurityDepositRequest request) {
        SecurityDeposit deposit = new SecurityDeposit();
        deposit.setLeaseId(request.getLeaseId());
        deposit.setAmount(request.getAmount());
        deposit.setStatus("HELD");
        depositRepository.save(deposit);
    }

    @Transactional
    public void refundDeposit(String leaseId, BigDecimal amount, String reason) {
        // Find deposit and process refund
    }
}