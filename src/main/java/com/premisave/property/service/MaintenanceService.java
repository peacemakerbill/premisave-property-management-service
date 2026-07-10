package com.premisave.property.service;

import com.premisave.property.dto.request.MaintenanceRequest;
import com.premisave.property.dto.request.UpdateMaintenanceStatusRequest;
import com.premisave.property.dto.response.MaintenanceResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.entity.Maintenance;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.MaintenanceRequestRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private static final int DUPLICATE_WINDOW_HOURS = 24;

    private final MaintenanceRequestRepository maintenanceRepository;
    private final RentalUnitRepository rentalUnitRepository;

    @Transactional
    public MaintenanceResponse createMaintenanceRequest(MaintenanceRequest request, String tenantId) {
        if (request.getRentalUnitId() == null || request.getRentalUnitId().isBlank()) {
            throw new BadRequestException("rentalUnitId is required");
        }

        List<Maintenance> recentDuplicates = maintenanceRepository
                .findByTenantIdAndRentalUnitIdAndTitleIgnoreCaseAndCreatedAtAfter(
                        tenantId, request.getRentalUnitId(), request.getTitle(),
                        LocalDateTime.now().minusHours(DUPLICATE_WINDOW_HOURS));

        if (!recentDuplicates.isEmpty()) {
            throw new ConflictException(
                    "You've already submitted a maintenance request titled '" + request.getTitle()
                            + "' for this unit in the last 24 hours. Please wait before submitting again, "
                            + "or check the status of your existing request.");
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

        if (maintenance.getRentalUnitId() != null) {
            rentalUnitRepository.findById(maintenance.getRentalUnitId())
                    .ifPresent(unit -> response.setRentalUnit(toRentalUnitSummary(unit)));
        }

        return response;
    }

    private RentalUnitSummaryResponse toRentalUnitSummary(RentalUnit unit) {
        RentalUnitSummaryResponse summary = new RentalUnitSummaryResponse();
        summary.setId(unit.getId());
        summary.setUnitNumber(unit.getUnitNumber());
        summary.setFloor(unit.getFloor());
        summary.setRentAmount(unit.getRentAmount());
        summary.setStatus(unit.getStatus());
        return summary;
    }
}