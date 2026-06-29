package com.premisave.property.service;

import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.repository.OccupancyHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OccupancyService {

    private final OccupancyHistoryRepository occupancyHistoryRepository;

    @Transactional
    public void recordMoveIn(String rentalUnitId, String tenantId, String leaseId) {
        OccupancyHistory history = new OccupancyHistory();
        history.setRentalUnitId(rentalUnitId);
        history.setTenantId(tenantId);
        history.setLeaseId(leaseId);
        history.setMoveInDate(LocalDateTime.now());
        occupancyHistoryRepository.save(history);
    }

    @Transactional
    public void recordMoveOut(String rentalUnitId, String tenantId) {
        // Find current occupancy and set move out date
    }

    public List<OccupancyHistory> getUnitHistory(String rentalUnitId) {
        return occupancyHistoryRepository.findByRentalUnitId(rentalUnitId);
    }
}