package com.premisave.property.service;

import com.premisave.property.dto.request.InspectionRequest;
import com.premisave.property.entity.Inspection;
import com.premisave.property.repository.InspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;

    public void createInspection(InspectionRequest request) {
        Inspection inspection = new Inspection();
        // map fields
        inspectionRepository.save(inspection);
    }
}