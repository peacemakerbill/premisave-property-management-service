package com.premisave.property.service;

import com.premisave.property.dto.request.MaintenanceRequest;
import com.premisave.property.dto.response.MaintenanceResponse;
import com.premisave.property.entity.Maintenance;
import com.premisave.property.mapper.MaintenanceMapper;
import com.premisave.property.repository.MaintenanceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRequestRepository maintenanceRepository;
    private final MaintenanceMapper maintenanceMapper;

    public MaintenanceResponse createMaintenanceRequest(MaintenanceRequest request, String tenantId) {
        Maintenance maintenance = maintenanceMapper.toEntity(request);
        // Set tenant ID, etc.
        Maintenance saved = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(saved);
    }
}