package com.premisave.property.service;

import com.premisave.property.dto.request.InspectionRequest;
import com.premisave.property.dto.request.UpdateInspectionRequest;
import com.premisave.property.dto.response.InspectionResponse;
import com.premisave.property.entity.Inspection;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.InspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;

    @Transactional
    public InspectionResponse createInspection(InspectionRequest request, String inspectorId) {
        if (request.getRentalUnitId() == null || request.getRentalUnitId().isBlank()) {
            throw new BadRequestException("rentalUnitId is required");
        }

        Inspection inspection = new Inspection();
        inspection.setRentalUnitId(request.getRentalUnitId());
        inspection.setInspectorId(inspectorId);
        inspection.setTitle(request.getTitle());
        inspection.setFindings(request.getFindings());
        inspection.setRecommendations(request.getRecommendations());
        inspection.setInspectionDate(request.getInspectionDate());

        return toResponse(inspectionRepository.save(inspection));
    }

    public InspectionResponse getInspection(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<InspectionResponse> getInspectionsByUnit(String rentalUnitId) {
        return inspectionRepository.findByRentalUnitId(rentalUnitId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<InspectionResponse> getInspectionsByInspector(String inspectorId) {
        return inspectionRepository.findByInspectorId(inspectorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InspectionResponse updateFindings(String id, UpdateInspectionRequest request) {
        Inspection inspection = findOrThrow(id);

        if (request.getFindings() != null) {
            inspection.setFindings(request.getFindings());
        }
        if (request.getRecommendations() != null) {
            inspection.setRecommendations(request.getRecommendations());
        }

        return toResponse(inspectionRepository.save(inspection));
    }

    private Inspection findOrThrow(String id) {
        return inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
    }

    private InspectionResponse toResponse(Inspection inspection) {
        InspectionResponse response = new InspectionResponse();
        response.setId(inspection.getId());
        response.setRentalUnitId(inspection.getRentalUnitId());
        response.setInspectorId(inspection.getInspectorId());
        response.setTitle(inspection.getTitle());
        response.setFindings(inspection.getFindings());
        response.setRecommendations(inspection.getRecommendations());
        response.setInspectionDate(inspection.getInspectionDate());
        return response;
    }
}