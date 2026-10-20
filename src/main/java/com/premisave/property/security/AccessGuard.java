package com.premisave.property.security;

import com.premisave.property.entity.Inspection;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.Maintenance;
import com.premisave.property.entity.MeterReading;
import com.premisave.property.entity.Notice;
import com.premisave.property.entity.Owner;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.WorkOrder;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.LeaseType;
import com.premisave.property.enums.MaintenanceStatus;
import com.premisave.property.exception.ForbiddenException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.UnauthorizedException;
import com.premisave.property.repository.InspectionRepository;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.MaintenanceRequestRepository;
import com.premisave.property.repository.MeterReadingRepository;
import com.premisave.property.repository.NoticeRepository;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.OwnerRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import com.premisave.property.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One place that answers "is this caller allowed to touch this record?".
 *
 * Who the caller is comes from the JWT (JwtAuthFilter puts the auth-service
 * userId on the request). What they may touch comes from the data:
 *
 *  - manager:     an ADMIN, or the home owner whose Property the record belongs to
 *  - participant: a manager, or the tenant the record is about
 *
 * A role such as HOME_OWNER only says the caller is an owner, never that they
 * own THIS property, so every check here walks record -> unit -> property -> owner.
 *
 * Missing records are reported as 404 first, so a caller learns "forbidden" only
 * for something that exists.
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private static final Set<LeaseStatus> LIVE_LEASES = EnumSet.of(LeaseStatus.ACTIVE, LeaseStatus.RENEWED);

    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;
    private final OwnerRepository ownerRepository;
    private final TenantRepository tenantRepository;
    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final NoticeRepository noticeRepository;
    private final InspectionRepository inspectionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final MeterReadingRepository meterReadingRepository;

    // ------------------------------------------------------------------
    // Who is calling
    // ------------------------------------------------------------------

    public String currentUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object userId = attributes == null ? null : attributes.getAttribute("userId", RequestAttributes.SCOPE_REQUEST);
        if (userId instanceof String id && !id.isBlank()) {
            return id;
        }
        throw new UnauthorizedException("Please sign in to continue");
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** The caller's Owner document id, if they have an owner profile. */
    public Optional<String> currentOwnerId() {
        return ownerRepository.findByUserId(currentUserId()).map(Owner::getId);
    }

    /** The caller's Tenant document id, if they have a tenant profile. */
    public Optional<String> currentTenantId() {
        return tenantRepository.findByUserId(currentUserId()).map(t -> t.getId());
    }

    public String requireOwnerId() {
        return currentOwnerId().orElseThrow(() ->
                new ForbiddenException("Only registered home owners can do this. Create your owner profile first."));
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("This action is limited to Premisave administrators");
        }
    }

    /** ids of every property the caller owns (empty for non-owners). */
    public Set<String> ownedPropertyIds() {
        return currentOwnerId()
                .map(ownerId -> propertyRepository.findByOwnerId(ownerId).stream()
                        .map(Property::getId)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /** ids of every rental unit in the caller's properties. */
    public Set<String> ownedUnitIds() {
        Set<String> propertyIds = ownedPropertyIds();
        if (propertyIds.isEmpty()) {
            return Set.of();
        }
        return rentalUnitRepository.findByPropertyIdIn(List.copyOf(propertyIds)).stream()
                .map(RentalUnit::getId)
                .collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------
    // Property / unit / lease: manager checks
    // ------------------------------------------------------------------

    public Property requirePropertyManager(String propertyId) {
        Property property = findProperty(propertyId);
        if (!isAdmin() && !ownsProperty(property)) {
            throw new ForbiddenException("You can only manage your own properties");
        }
        return property;
    }

    public RentalUnit requireUnitManager(String unitId) {
        RentalUnit unit = findUnit(unitId);
        requirePropertyManager(unit.getPropertyId());
        return unit;
    }

    public Lease requireLeaseManager(String leaseId) {
        Lease lease = findLease(leaseId);
        requirePropertyManager(lease.getPropertyId());
        return lease;
    }

    /** For requests that name either a unit or a whole property (lease creation). */
    public void requireManagerOfTarget(String rentalUnitId, String propertyId) {
        if (rentalUnitId != null && !rentalUnitId.isBlank()) {
            requireUnitManager(rentalUnitId);
        } else if (propertyId != null && !propertyId.isBlank()) {
            requirePropertyManager(propertyId);
        }
        // neither given: the service rejects the request as a 400
    }

    // ------------------------------------------------------------------
    // Participant checks: a manager, or the tenant concerned
    // ------------------------------------------------------------------

    public Lease requireLeaseParticipant(String leaseId) {
        Lease lease = findLease(leaseId);
        if (isAdmin() || ownsProperty(lease.getPropertyId())) {
            return lease;
        }
        boolean isLeaseTenant = currentTenantId().map(id -> id.equals(lease.getTenantId())).orElse(false);
        if (!isLeaseTenant) {
            throw new ForbiddenException("You do not have access to this lease");
        }
        return lease;
    }

    /** Manager of the unit's property, or a tenant who currently occupies the unit. */
    public RentalUnit requireUnitParticipant(String unitId) {
        RentalUnit unit = findUnit(unitId);
        if (isAdmin() || ownsProperty(unit.getPropertyId())) {
            return unit;
        }
        boolean occupies = currentTenantId().map(id -> isTenantOnUnit(id, unit)).orElse(false);
        if (!occupies) {
            throw new ForbiddenException("You do not have access to this unit");
        }
        return unit;
    }

    /** Manager of the property, or a tenant who currently leases or occupies part of it. */
    public Property requirePropertyParticipant(String propertyId) {
        Property property = findProperty(propertyId);
        if (isAdmin() || ownsProperty(property)) {
            return property;
        }
        boolean onProperty = currentTenantId().map(id -> isTenantOnProperty(id, propertyId)).orElse(false);
        if (!onProperty) {
            throw new ForbiddenException("You do not have access to this property");
        }
        return property;
    }

    // ------------------------------------------------------------------
    // Tenant-scoped data
    // ------------------------------------------------------------------

    /** Only the tenant themselves (or an admin). */
    public void requireTenantSelf(String tenantId) {
        if (isAdmin()) {
            return;
        }
        boolean self = currentTenantId().map(id -> id.equals(tenantId)).orElse(false);
        if (!self) {
            throw new ForbiddenException("You can only view your own records");
        }
    }

    /** The tenant themselves, an admin, or an owner who has (or had) this tenant on one of their properties. */
    public void requireTenantSelfOrLandlord(String tenantId) {
        if (isAdmin() || isSelf(tenantId) || isLandlordOf(tenantId)) {
            return;
        }
        throw new ForbiddenException("You do not have access to this tenant's records");
    }

    /** An owner (or admin) with a tenancy relationship to this tenant. */
    public void requireTenantLandlord(String tenantId) {
        if (isAdmin() || isLandlordOf(tenantId)) {
            return;
        }
        throw new ForbiddenException("You can only do this for tenants who rent from you");
    }

    /**
     * True when the caller may see ALL of a tenant's records (the tenant, or an admin).
     * A landlord passing requireTenantSelfOrLandlord sees only the records tied to
     * their own properties, so list endpoints filter for them.
     */
    public boolean seesAllOf(String tenantId) {
        return isAdmin() || isSelf(tenantId);
    }

    public boolean isSelf(String tenantId) {
        return currentTenantId().map(id -> id.equals(tenantId)).orElse(false);
    }

    public boolean isLandlordOf(String tenantId) {
        Set<String> owned = ownedPropertyIds();
        if (owned.isEmpty()) {
            return false;
        }
        boolean viaLease = leaseRepository.findByTenantId(tenantId).stream()
                .anyMatch(l -> owned.contains(l.getPropertyId()));
        return viaLease || occupancyHistoryRepository.findByTenantId(tenantId).stream()
                .anyMatch(o -> owned.contains(o.getPropertyId()));
    }

    // ------------------------------------------------------------------
    // Record-level checks (the controller only has an id)
    // ------------------------------------------------------------------

    public Maintenance requireMaintenanceViewer(String maintenanceId) {
        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));
        if (isSelf(maintenance.getTenantId())) {
            return maintenance;
        }
        requireUnitManager(maintenance.getRentalUnitId());
        return maintenance;
    }

    public Maintenance requireMaintenanceManager(String maintenanceId) {
        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found"));
        requireUnitManager(maintenance.getRentalUnitId());
        return maintenance;
    }

    /** Ids of maintenance requests raised on the caller's units (any status). */
    public Set<String> ownedMaintenanceIds() {
        Set<String> unitIds = ownedUnitIds();
        if (unitIds.isEmpty()) {
            return Set.of();
        }
        return maintenanceRepository
                .findByRentalUnitIdInAndStatusIn(List.copyOf(unitIds), List.of(MaintenanceStatus.values()))
                .stream()
                .map(Maintenance::getId)
                .collect(Collectors.toSet());
    }

    /** The assigned technician, or whoever manages the unit the work is on. */
    public WorkOrder requireWorkOrderUpdater(String workOrderId) {
        WorkOrder workOrder = findWorkOrder(workOrderId);
        if (currentUserId().equals(workOrder.getAssignedTo())) {
            return workOrder;
        }
        requireMaintenanceManager(workOrder.getMaintenanceRequestId());
        return workOrder;
    }

    /** The assigned technician, the unit's manager, or the tenant who raised the request. */
    public WorkOrder requireWorkOrderViewer(String workOrderId) {
        WorkOrder workOrder = findWorkOrder(workOrderId);
        if (currentUserId().equals(workOrder.getAssignedTo())) {
            return workOrder;
        }
        requireMaintenanceViewer(workOrder.getMaintenanceRequestId());
        return workOrder;
    }

    public MeterReading requireMeterReadingManager(String meterReadingId) {
        MeterReading reading = meterReadingRepository.findById(meterReadingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found"));
        requireUnitManager(reading.getRentalUnitId());
        return reading;
    }

    /** Owner profiles carry bank details, so only the owner themselves (or an admin) may read one by id. */
    public void requireOwnerSelf(String ownerId) {
        if (isAdmin()) {
            return;
        }
        boolean self = currentOwnerId().map(id -> id.equals(ownerId)).orElse(false);
        if (!self) {
            throw new ForbiddenException("You can only view your own owner profile");
        }
    }

    public Notice requireNoticeViewer(String noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"));
        if (isAdmin() || isSelf(notice.getTenantId()) || ownsProperty(notice.getPropertyId())) {
            return notice;
        }
        throw new ForbiddenException("You do not have access to this notice");
    }

    public Inspection requireInspectionViewer(String inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
        String userId = currentUserId();
        if (userId.equals(inspection.getCreatedByUserId()) || userId.equals(inspection.getInspectorUserId())) {
            return inspection;
        }
        requireUnitManager(inspection.getRentalUnitId());
        return inspection;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private boolean ownsProperty(Property property) {
        return currentOwnerId().map(id -> id.equals(property.getOwnerId())).orElse(false);
    }

    private boolean ownsProperty(String propertyId) {
        if (propertyId == null) {
            return false;
        }
        return propertyRepository.findById(propertyId).map(this::ownsProperty).orElse(false);
    }

    private boolean isTenantOnUnit(String tenantId, RentalUnit unit) {
        boolean unitLease = leaseRepository.findByRentalUnitId(unit.getId()).stream()
                .anyMatch(l -> tenantId.equals(l.getTenantId()) && LIVE_LEASES.contains(l.getStatus()));
        if (unitLease) {
            return true;
        }
        if (occupancyHistoryRepository
                .findByRentalUnitIdAndTenantIdAndMoveOutDateIsNull(unit.getId(), tenantId).isPresent()) {
            return true;
        }
        return hasWholePropertyLease(tenantId, unit.getPropertyId());
    }

    private boolean isTenantOnProperty(String tenantId, String propertyId) {
        if (hasWholePropertyLease(tenantId, propertyId)) {
            return true;
        }
        return rentalUnitRepository.findByPropertyId(propertyId).stream()
                .anyMatch(unit -> isTenantOnUnit(tenantId, unit));
    }

    private boolean hasWholePropertyLease(String tenantId, String propertyId) {
        if (propertyId == null) {
            return false;
        }
        return leaseRepository.findByPropertyIdIn(List.of(propertyId)).stream()
                .anyMatch(l -> l.getLeaseType() == LeaseType.WHOLE_PROPERTY
                        && tenantId.equals(l.getTenantId())
                        && LIVE_LEASES.contains(l.getStatus()));
    }

    private WorkOrder findWorkOrder(String workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found"));
    }

    private Property findProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }

    private RentalUnit findUnit(String unitId) {
        return rentalUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));
    }

    private Lease findLease(String leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
    }
}
