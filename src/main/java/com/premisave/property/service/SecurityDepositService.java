package com.premisave.property.service;

import com.premisave.property.dto.request.RefundDepositRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.SecurityDepositResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.SecurityDeposit;
import com.premisave.property.enums.DepositStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.SecurityDepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SecurityDepositService {

    private final SecurityDepositRepository depositRepository;
    private final LeaseRepository leaseRepository;

    @Transactional
    public SecurityDepositResponse holdDeposit(SecurityDepositRequest request) {
        boolean hasLease = request.getLeaseId() != null && !request.getLeaseId().isBlank();
        boolean hasUnit = request.getRentalUnitId() != null && !request.getRentalUnitId().isBlank();

        if (hasLease == hasUnit) {
            throw new BadRequestException("Provide exactly one of leaseId or rentalUnitId");
        }

        return hasLease ? holdLeaseDeposit(request) : holdUnitDeposit(request);
    }

    private SecurityDepositResponse holdLeaseDeposit(SecurityDepositRequest request) {
        depositRepository.findByLeaseId(request.getLeaseId()).ifPresent(existing -> {
            throw new ConflictException("A security deposit already exists for this lease");
        });

        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        SecurityDeposit deposit = new SecurityDeposit();
        deposit.setLeaseId(request.getLeaseId());
        deposit.setTenantId(lease.getTenantId());
        deposit.setAmount(request.getAmount());
        deposit.setRefundedAmount(BigDecimal.ZERO);
        deposit.setStatus(DepositStatus.HELD);

        return toResponse(depositRepository.save(deposit));
    }

    private SecurityDepositResponse holdUnitDeposit(SecurityDepositRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new BadRequestException(
                    "tenantId is required when holding a deposit against a rentalUnitId");
        }

        depositRepository.findByRentalUnitIdAndTenantId(request.getRentalUnitId(), request.getTenantId())
                .ifPresent(existing -> {
                    throw new ConflictException("A security deposit already exists for this tenant on this unit");
                });

        SecurityDeposit deposit = new SecurityDeposit();
        deposit.setRentalUnitId(request.getRentalUnitId());
        deposit.setTenantId(request.getTenantId());
        deposit.setAmount(request.getAmount());
        deposit.setRefundedAmount(BigDecimal.ZERO);
        deposit.setStatus(DepositStatus.HELD);

        return toResponse(depositRepository.save(deposit));
    }

    @Transactional
    public SecurityDepositResponse refundDeposit(RefundDepositRequest request) {
        boolean hasLease = request.getLeaseId() != null && !request.getLeaseId().isBlank();
        boolean hasUnit = request.getRentalUnitId() != null && !request.getRentalUnitId().isBlank();

        if (hasLease == hasUnit) {
            throw new BadRequestException("Provide exactly one of leaseId or rentalUnitId");
        }

        SecurityDeposit deposit = hasLease
                ? depositRepository.findByLeaseId(request.getLeaseId())
                        .orElseThrow(() -> new ResourceNotFoundException("No security deposit found for this lease"))
                : depositRepository.findByRentalUnitIdAndTenantId(request.getRentalUnitId(), requireTenantId(request))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No security deposit found for this tenant on this unit"));

        if (deposit.getStatus() == DepositStatus.REFUNDED) {
            throw new ConflictException("This deposit has already been fully refunded");
        }
        if (request.getAmount().compareTo(deposit.getAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed the held deposit amount");
        }

        deposit.setRefundedAmount(request.getAmount());
        deposit.setRefundedAt(LocalDateTime.now());
        deposit.setStatus(request.getAmount().compareTo(deposit.getAmount()) == 0
                ? DepositStatus.REFUNDED
                : DepositStatus.PARTIALLY_REFUNDED);

        return toResponse(depositRepository.save(deposit));
    }

    public SecurityDepositResponse getDepositByLease(String leaseId) {
        return depositRepository.findByLeaseId(leaseId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No security deposit found for this lease"));
    }

    public SecurityDepositResponse getDepositByUnit(String rentalUnitId, String tenantId) {
        return depositRepository.findByRentalUnitIdAndTenantId(rentalUnitId, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No security deposit found for this tenant on this unit"));
    }

    private String requireTenantId(RefundDepositRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new BadRequestException("tenantId is required when refunding a deposit for a rentalUnitId");
        }
        return request.getTenantId();
    }

    private SecurityDepositResponse toResponse(SecurityDeposit deposit) {
        SecurityDepositResponse response = new SecurityDepositResponse();
        response.setId(deposit.getId());
        response.setLeaseId(deposit.getLeaseId());
        response.setRentalUnitId(deposit.getRentalUnitId());
        response.setTenantId(deposit.getTenantId());
        response.setAmount(deposit.getAmount());
        response.setRefundedAmount(deposit.getRefundedAmount());
        response.setStatus(deposit.getStatus());
        response.setRefundedAt(deposit.getRefundedAt());
        return response;
    }
}