package com.premisave.property.service;

import com.premisave.property.dto.response.LeaseSummaryResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentScheduleResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentSchedule;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentScheduleRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentScheduleService {

    private final RentScheduleRepository rentScheduleRepository;
    private final LeaseRepository leaseRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public List<RentSchedule> generateMonthlySchedule(String leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        List<RentSchedule> schedules = new ArrayList<>();
        LocalDate dueDate = lease.getStartDate();

        while (!dueDate.isAfter(lease.getEndDate())) {
            RentSchedule schedule = new RentSchedule();
            schedule.setLeaseId(lease.getId());
            schedule.setDueDate(dueDate);
            schedule.setAmountDue(lease.getMonthlyRent());
            schedule.setAmountPaid(BigDecimal.ZERO);
            schedule.setStatus(PaymentStatus.PENDING);
            schedules.add(schedule);
            dueDate = dueDate.plusMonths(1);
        }

        return rentScheduleRepository.saveAll(schedules);
    }

    public List<RentScheduleResponse> getUpcomingPayments(String leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        // Resolved ONCE per request, not per schedule entry — every entry
        // in this list belongs to the same lease, so there's no reason to
        // repeat these lookups across (potentially) dozens of entries.
        TenantSummaryResponse tenantSummary = tenantRepository.findById(lease.getTenantId())
                .map(this::toTenantSummary).orElse(null);
        LeaseSummaryResponse leaseSummary = toLeaseSummary(lease);
        RentalUnitSummaryResponse unitSummary = lease.getRentalUnitId() != null
                ? rentalUnitRepository.findById(lease.getRentalUnitId()).map(this::toRentalUnitSummary).orElse(null)
                : null; // whole-property lease — no specific unit
        PropertySummaryResponse propertySummary = lease.getPropertyId() != null
                ? propertyRepository.findById(lease.getPropertyId()).map(this::toPropertySummary).orElse(null)
                : null;

        return rentScheduleRepository.findByLeaseId(leaseId).stream()
                .map(schedule -> toResponse(schedule, tenantSummary, leaseSummary, propertySummary, unitSummary))
                .toList();
    }

    // Runs daily to flag unpaid schedule entries that are now past due
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void markOverdueSchedules() {
        List<RentSchedule> overdue = rentScheduleRepository.findByDueDateBeforeAndStatusIn(
                LocalDate.now(), List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID));

        overdue.forEach(schedule -> schedule.setStatus(PaymentStatus.OVERDUE));
        rentScheduleRepository.saveAll(overdue);
    }

    private RentScheduleResponse toResponse(RentSchedule schedule, TenantSummaryResponse tenantSummary,
                                             LeaseSummaryResponse leaseSummary,
                                             PropertySummaryResponse propertySummary,
                                             RentalUnitSummaryResponse unitSummary) {
        RentScheduleResponse response = new RentScheduleResponse();
        response.setId(schedule.getId());
        response.setDueDate(schedule.getDueDate());
        response.setAmountDue(schedule.getAmountDue());
        response.setAmountPaid(schedule.getAmountPaid());
        response.setStatus(schedule.getStatus());

        response.setTenant(tenantSummary);
        response.setLease(leaseSummary);
        response.setProperty(propertySummary);
        response.setUnit(unitSummary);

        applyPaymentSummary(response, schedule);

        return response;
    }

    /**
     * Computes balanceDue / overpaidAmount / a human-readable paymentMessage
     * from amountDue, amountPaid, and status — always accurate on any read,
     * however many payments have landed on this schedule entry over time.
     */
    private void applyPaymentSummary(RentScheduleResponse response, RentSchedule schedule) {
        BigDecimal amountDue = schedule.getAmountDue() != null ? schedule.getAmountDue() : BigDecimal.ZERO;
        BigDecimal amountPaid = schedule.getAmountPaid() != null ? schedule.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal difference = amountPaid.subtract(amountDue); // positive = overpaid, negative = balance owed

        BigDecimal balanceDue = difference.compareTo(BigDecimal.ZERO) < 0
                ? difference.negate()
                : BigDecimal.ZERO;
        BigDecimal overpaidAmount = difference.compareTo(BigDecimal.ZERO) > 0
                ? difference
                : BigDecimal.ZERO;

        response.setBalanceDue(balanceDue);
        response.setOverpaidAmount(overpaidAmount);
        response.setPaymentMessage(buildPaymentMessage(schedule.getStatus(), amountDue, balanceDue, overpaidAmount));
    }

    private String buildPaymentMessage(PaymentStatus status, BigDecimal amountDue,
                                        BigDecimal balanceDue, BigDecimal overpaidAmount) {
        return switch (status) {
            case PAID -> "This rent period has been paid in full.";
            case OVERPAID -> "This rent period was overpaid by KES " + overpaidAmount
                    + ". Please contact your property owner regarding a credit or refund.";
            case PARTIALLY_PAID -> "Partial payment received. KES " + balanceDue + " is still outstanding.";
            case OVERDUE -> "This rent period is overdue. KES " + balanceDue + " is outstanding.";
            case PENDING -> "No payment has been made yet. KES " + amountDue + " is due.";
            case FAILED -> "The last payment attempt for this period failed. Please try again.";
            case REFUNDED -> "This rent period's payment has been refunded.";
        };
    }

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        TenantSummaryResponse summary = new TenantSummaryResponse();
        summary.setId(tenant.getId());
        summary.setFullName(tenant.getFullName());
        summary.setPhoneNumber(tenant.getPhoneNumber());
        summary.setEmail(tenant.getEmail());
        return summary;
    }

    private LeaseSummaryResponse toLeaseSummary(Lease lease) {
        LeaseSummaryResponse summary = new LeaseSummaryResponse();
        summary.setId(lease.getId());
        summary.setLeaseType(lease.getLeaseType());
        summary.setStartDate(lease.getStartDate());
        summary.setEndDate(lease.getEndDate());
        summary.setMonthlyRent(lease.getMonthlyRent());
        summary.setStatus(lease.getStatus());
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

    private RentalUnitSummaryResponse toRentalUnitSummary(RentalUnit unit) {
        RentalUnitSummaryResponse summary = new RentalUnitSummaryResponse();
        summary.setId(unit.getId());
        summary.setUnitNumber(unit.getUnitNumber());
        summary.setFloor(unit.getFloor());
        summary.setRentAmount(unit.getRentAmount());
        summary.setStatus(unit.getStatus());
        return summary;
    }

    private com.premisave.property.dto.response.AddressResponse toAddressResponse(
            com.premisave.property.entity.Address address) {
        if (address == null) {
            return null;
        }
        com.premisave.property.dto.response.AddressResponse response =
                new com.premisave.property.dto.response.AddressResponse();
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setLandmark(address.getLandmark());
        return response;
    }
}