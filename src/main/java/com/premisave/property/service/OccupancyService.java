package com.premisave.property.service;

import com.premisave.property.dto.response.OccupancyResponse;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OccupancyService {

    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final RentalUnitRepository rentalUnitRepository;

    @Transactional
    public OccupancyResponse recordMoveIn(String rentalUnitId, String tenantId, String leaseId) {
        OccupancyHistory history = new OccupancyHistory();
        history.setRentalUnitId(rentalUnitId);
        history.setTenantId(tenantId);
        history.setLeaseId(leaseId);
        history.setMoveInDate(LocalDateTime.now());

        return toResponse(occupancyHistoryRepository.save(history));
    }

    @Transactional
    public OccupancyResponse recordMoveOut(String rentalUnitId, String tenantId) {
        OccupancyHistory history = occupancyHistoryRepository
                .findByRentalUnitIdAndTenantIdAndMoveOutDateIsNull(rentalUnitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active occupancy found for this tenant and unit"));

        history.setMoveOutDate(LocalDateTime.now());
        OccupancyHistory saved = occupancyHistoryRepository.save(history);

        rentalUnitRepository.findById(rentalUnitId).ifPresent(unit -> {
            unit.setStatus(UnitStatus.VACANT);
            rentalUnitRepository.save(unit);
        });

        return toResponse(saved);
    }

    public List<OccupancyResponse> getUnitHistory(String rentalUnitId) {
        return occupancyHistoryRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OccupancyResponse> getTenantHistory(String tenantId) {
        return occupancyHistoryRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public OccupancyResponse getCurrentOccupancy(String rentalUnitId) {
        return occupancyHistoryRepository.findByRentalUnitIdAndMoveOutDateIsNull(rentalUnitId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("This unit is currently vacant"));
    }

    private OccupancyResponse toResponse(OccupancyHistory history) {
        OccupancyResponse response = new OccupancyResponse();
        response.setId(history.getId());
        response.setRentalUnitId(history.getRentalUnitId());
        response.setTenantId(history.getTenantId());
        response.setLeaseId(history.getLeaseId());
        response.setMoveInDate(history.getMoveInDate());
        response.setMoveOutDate(history.getMoveOutDate());
        return response;
    }
}