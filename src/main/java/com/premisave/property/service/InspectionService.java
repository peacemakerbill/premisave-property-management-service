package com.premisave.property.service;

import com.premisave.property.dto.request.CompleteInspectionRequest;
import com.premisave.property.dto.request.CreateInspectionRequest;
import com.premisave.property.dto.response.InspectionResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.entity.Inspection;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.enums.InspectionStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.UnauthorizedException;
import com.premisave.property.repository.InspectionRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final RentalUnitRepository rentalUnitRepository;

    @Transactional
    public InspectionResponse createInspection(CreateInspectionRequest request, String createdByUserId) {
        boolean hasManualInspectorDetails = request.getInspectorFullName() != null
                && !request.getInspectorFullName().isBlank();

        if (hasManualInspectorDetails
                && (request.getInspectorPhoneNumber() == null || request.getInspectorPhoneNumber().isBlank()
                    || request.getInspectorIdNumber() == null || request.getInspectorIdNumber().isBlank())) {
            throw new BadRequestException(
                    "inspectorFullName, inspectorPhoneNumber, and inspectorIdNumber are all required "
                            + "when assigning an inspector");
        }

        List<Inspection> sameDayInspections = inspectionRepository
                .findByRentalUnitIdAndScheduledDateAndStatusNot(
                        request.getRentalUnitId(), request.getScheduledDate(), InspectionStatus.CANCELLED);

        if (!sameDayInspections.isEmpty()) {
            throw new ConflictException(
                    "An inspection is already scheduled for this rental unit on " + request.getScheduledDate());
        }

        Inspection inspection = new Inspection();
        inspection.setRentalUnitId(request.getRentalUnitId());
        inspection.setTitle(request.getTitle());
        inspection.setScheduledDate(request.getScheduledDate());
        inspection.setCreatedByUserId(createdByUserId);
        inspection.setStatus(InspectionStatus.SCHEDULED);

        if (hasManualInspectorDetails) {
            inspection.setInspectorFullName(request.getInspectorFullName());
            inspection.setInspectorPhoneNumber(request.getInspectorPhoneNumber());
            inspection.setInspectorIdNumber(request.getInspectorIdNumber());
            inspection.setInspectorEmail(request.getInspectorEmail());
        }

        return toResponse(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionResponse completeInspection(String id, CompleteInspectionRequest request, String userId) {
        Inspection inspection = findOrThrow(id);

        if (inspection.getStatus() != InspectionStatus.SCHEDULED) {
            throw new ConflictException("Only a SCHEDULED inspection can be marked as completed");
        }

        boolean isCreator = userId.equals(inspection.getCreatedByUserId());
        boolean isAssignedInspector = inspection.getInspectorUserId() != null
                && userId.equals(inspection.getInspectorUserId());

        if (!isCreator && !isAssignedInspector) {
            throw new UnauthorizedException(
                    "Only the home owner who scheduled this inspection or the assigned inspector can complete it");
        }

        inspection.setFindings(request.getFindings());
        inspection.setRecommendations(request.getRecommendations());
        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setCompletedAt(LocalDateTime.now());
        inspection.setCompletedByUserId(userId);

        return toResponse(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionResponse cancelInspection(String id, String userId) {
        Inspection inspection = findOrThrow(id);

        if (inspection.getStatus() != InspectionStatus.SCHEDULED) {
            throw new ConflictException("Only a SCHEDULED inspection can be cancelled");
        }

        if (!userId.equals(inspection.getCreatedByUserId())) {
            throw new UnauthorizedException("Only the home owner who scheduled this inspection can cancel it");
        }

        inspection.setStatus(InspectionStatus.CANCELLED);
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

    public List<InspectionResponse> getInspectionsByInspector(String inspectorUserId, String callerUserId) {
        boolean isSelf = inspectorUserId.equals(callerUserId);
        boolean isHomeOwner = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOME_OWNER"));

        if (!isSelf && !isHomeOwner) {
            throw new UnauthorizedException("You can only view your own assigned inspections");
        }

        return inspectionRepository.findByInspectorUserId(inspectorUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<InspectionResponse> getInspectionsCreatedBy(String createdByUserId, String callerUserId) {
        if (!createdByUserId.equals(callerUserId)) {
            throw new UnauthorizedException("You can only view inspections you created");
        }

        return inspectionRepository.findByCreatedByUserId(createdByUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<InspectionResponse> getMyInspectionsByStatus(String createdByUserId, InspectionStatus status) {
        return inspectionRepository.findByCreatedByUserIdAndStatus(createdByUserId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    private Inspection findOrThrow(String id) {
        return inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
    }

    private InspectionResponse toResponse(Inspection inspection) {
        InspectionResponse response = new InspectionResponse();
        response.setId(inspection.getId());
        response.setRentalUnitId(inspection.getRentalUnitId());
        response.setCreatedByUserId(inspection.getCreatedByUserId());
        response.setInspectorFullName(inspection.getInspectorFullName());
        response.setInspectorPhoneNumber(inspection.getInspectorPhoneNumber());
        response.setInspectorIdNumber(inspection.getInspectorIdNumber());
        response.setInspectorEmail(inspection.getInspectorEmail());
        response.setInspectorUserId(inspection.getInspectorUserId());
        response.setTitle(inspection.getTitle());
        response.setScheduledDate(inspection.getScheduledDate());
        response.setStatus(inspection.getStatus());
        response.setFindings(inspection.getFindings());
        response.setRecommendations(inspection.getRecommendations());
        response.setCompletedAt(inspection.getCompletedAt());
        response.setCompletedByUserId(inspection.getCompletedByUserId());

        if (inspection.getRentalUnitId() != null) {
            rentalUnitRepository.findById(inspection.getRentalUnitId())
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