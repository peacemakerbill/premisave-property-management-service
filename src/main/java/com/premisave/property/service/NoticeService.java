package com.premisave.property.service;

import com.premisave.property.dto.request.LeaseNoticeRequest;
import com.premisave.property.dto.request.UnitNoticeRequest;
import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.NoticeResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.Notice;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.NoticeType;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.NoticeRepository;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final int DUPLICATE_WINDOW_HOURS = 24;

    private final NoticeRepository noticeRepository;
    private final TenantRepository tenantRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final PropertyRepository propertyRepository;
    private final OccupancyHistoryRepository occupancyHistoryRepository;

    // ------------------------------------------------------------------
    // Direct-unit notices — tenant occupying a rental unit with no lease
    // ------------------------------------------------------------------

    @Transactional
    public NoticeResponse sendUnitNotice(UnitNoticeRequest request) {
        RentalUnit unit = rentalUnitRepository.findById(request.getRentalUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));

        OccupancyHistory occupancy = occupancyHistoryRepository
                .findByRentalUnitIdAndMoveOutDateIsNull(request.getRentalUnitId())
                .orElseThrow(() -> new BadRequestException(
                        "This unit currently has no active tenant to notify"));

        if (occupancy.getLeaseId() != null) {
            throw new BadRequestException(
                    "This unit's current occupancy is lease-backed. Use the lease notice endpoint instead.");
        }

        String tenantId = occupancy.getTenantId();

        if (request.getTenantId() != null && !request.getTenantId().isBlank()
                && !request.getTenantId().equals(tenantId)) {
            throw new BadRequestException(
                    "The provided tenantId does not match the current occupant of this unit");
        }

        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        assertNoRecentDuplicate(tenantId, request.getNoticeType());

        Notice notice = new Notice();
        notice.setTenantId(tenantId);
        notice.setRentalUnitId(unit.getId());
        notice.setPropertyId(unit.getPropertyId());
        notice.setLeaseId(null);
        notice.setNoticeType(request.getNoticeType());
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setSentAt(LocalDateTime.now());

        return toResponse(noticeRepository.save(notice));
    }

    // ------------------------------------------------------------------
    // Lease-backed notices
    // ------------------------------------------------------------------

    @Transactional
    public NoticeResponse sendLeaseNotice(LeaseNoticeRequest request) {
        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        if (lease.getStatus() == LeaseStatus.DRAFT) {
            throw new BadRequestException(
                    "Cannot send a notice for a lease that hasn't started yet (DRAFT)");
        }

        tenantRepository.findById(lease.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        assertNoRecentDuplicate(lease.getTenantId(), request.getNoticeType());

        Notice notice = new Notice();
        notice.setTenantId(lease.getTenantId());
        notice.setLeaseId(lease.getId());
        notice.setRentalUnitId(lease.getRentalUnitId()); // null for whole-property leases
        notice.setPropertyId(lease.getPropertyId());
        notice.setNoticeType(request.getNoticeType());
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setSentAt(LocalDateTime.now());

        return toResponse(noticeRepository.save(notice));
    }

    private void assertNoRecentDuplicate(String tenantId, NoticeType noticeType) {
        List<Notice> recent = noticeRepository.findByTenantIdAndNoticeTypeAndSentAtAfter(
                tenantId, noticeType, LocalDateTime.now().minusHours(DUPLICATE_WINDOW_HOURS));

        if (!recent.isEmpty()) {
            throw new ConflictException(
                    "A " + noticeType + " notice has already been sent to this tenant in the last 24 hours");
        }
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    public NoticeResponse getNotice(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<NoticeResponse> getNoticesByTenant(String tenantId) {
        return noticeRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NoticeResponse> getNoticesByLease(String leaseId) {
        return noticeRepository.findByLeaseId(leaseId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NoticeResponse> getNoticesByTenantAndType(String tenantId, NoticeType noticeType) {
        return noticeRepository.findByTenantIdAndNoticeType(tenantId, noticeType).stream()
                .map(this::toResponse)
                .toList();
    }

    private Notice findOrThrow(String id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"));
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    private NoticeResponse toResponse(Notice notice) {
        NoticeResponse response = new NoticeResponse();
        response.setId(notice.getId());
        response.setTenantId(notice.getTenantId());
        response.setLeaseId(notice.getLeaseId());
        response.setRentalUnitId(notice.getRentalUnitId());
        response.setPropertyId(notice.getPropertyId());
        response.setNoticeType(notice.getNoticeType());
        response.setTitle(notice.getTitle());
        response.setContent(notice.getContent());
        response.setSentAt(notice.getSentAt());

        if (notice.getTenantId() != null) {
            tenantRepository.findById(notice.getTenantId())
                    .ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }

        if (notice.getRentalUnitId() != null) {
            rentalUnitRepository.findById(notice.getRentalUnitId())
                    .ifPresent(unit -> response.setRentalUnit(toRentalUnitSummary(unit)));
        }

        if (notice.getPropertyId() != null) {
            propertyRepository.findById(notice.getPropertyId())
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }

        return response;
    }

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        TenantSummaryResponse summary = new TenantSummaryResponse();
        summary.setId(tenant.getId());
        summary.setFullName(tenant.getFullName());
        summary.setPhoneNumber(tenant.getPhoneNumber());
        summary.setEmail(tenant.getEmail());
        return summary;
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

    private PropertySummaryResponse toPropertySummary(Property property) {
        PropertySummaryResponse summary = new PropertySummaryResponse();
        summary.setId(property.getId());
        summary.setTitle(property.getTitle());
        summary.setPropertyType(property.getPropertyType());
        summary.setAddress(toAddressResponse(property.getAddress()));
        summary.setRegistrationNumber(property.getRegistrationNumber());
        return summary;
    }

    private AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setLandmark(address.getLandmark());
        return response;
    }
}