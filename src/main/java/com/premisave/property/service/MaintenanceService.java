package com.premisave.property.service;

import com.premisave.property.dto.request.MaintenanceRequest;
import com.premisave.property.dto.request.UpdateMaintenanceStatusRequest;
import com.premisave.property.dto.response.MaintenanceResponse;
import com.premisave.property.entity.Maintenance;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.MaintenanceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRequestRepository maintenanceRepository;

    @Transactional
    public MaintenanceResponse createMaintenanceRequest(MaintenanceRequest request, String tenantId) {
        if (request.getRentalUnitId() == null || request.getRentalUnitId().isBlank()) {
            throw new BadRequestException("rentalUnitId is required");
        }

        Maintenance maintenance = new Maintenance();
        maintenance.setTenantId(tenantId);
        maintenance.setRentalUnitId(request.getRentalUnitId());
        maintenance.setTitle(request.getTitle());
        maintenance.setDescription(request.getDescription());
        maintenance.setStatus(MaintenanceStatus.PENDING);

        return toResponse(maintenanceRepository.save(maintenance));
    }

    public MaintenanceResponse getMaintenanceRequest(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<MaintenanceResponse> getRequestsByTenant(String tenantId) {
        return maintenanceRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MaintenanceResponse> getRequestsByUnit(String rentalUnitId) {
        return maintenanceRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MaintenanceResponse> getRequestsByStatus(MaintenanceStatus status) {
        return maintenanceRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MaintenanceResponse updateStatus(String id, UpdateMaintenanceStatusRequest request) {
        Maintenance maintenance = findOrThrow(id);
        maintenance.setStatus(request.getStatus());
        return toResponse(maintenanceRepository.save(maintenance));
    }

    private Maintenance findOrThrow(String id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));
    }

    private MaintenanceResponse toResponse(Maintenance maintenance) {
        MaintenanceResponse response = new MaintenanceResponse();
        response.setId(maintenance.getId());
        response.setTenantId(maintenance.getTenantId());
        response.setRentalUnitId(maintenance.getRentalUnitId());
        response.setTitle(maintenance.getTitle());
        response.setDescription(maintenance.getDescription());
        response.setStatus(maintenance.getStatus().name());
        response.setCreatedAt(maintenance.getCreatedAt());
        return response;
    }
}