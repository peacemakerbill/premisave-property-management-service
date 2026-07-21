package com.premisave.property.service;

import com.premisave.property.dto.response.LeaseSummaryResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentBalanceResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantRentBalanceSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentBalance;
import com.premisave.property.entity.RentSchedule;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.OccupancyHistoryRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentBalanceRepository;
import com.premisave.property.repository.RentScheduleRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentBalanceService {

    // Safety cap on how many months of back-charges a single catch-up can
    // apply at once (~2 years), guarding against a runaway loop.
    private static final int MAX_MONTHS_TO_BACK_CHARGE = 24;

    private final RentBalanceRepository rentBalanceRepository;
    private final RentScheduleRepository rentScheduleRepository;
    private final LeaseRepository leaseRepository;
    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;

    // ------------------------------------------------------------------
    // Lease-based balances — derived live from RentSchedule, never
    // persisted separately. A period that hasn't come due yet doesn't
    // count as "owed", so paying ahead naturally shows as a credit
    // (negative balance) until that period's due date arrives.
    // ------------------------------------------------------------------

    public RentBalanceResponse getLeaseBalance(String leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        List<RentSchedule> schedules = rentScheduleRepository.findByLeaseId(leaseId);
        LocalDate today = LocalDate.now();

        BigDecimal totalDueToDate = schedules.stream()
                .filter(s -> !s.getDueDate().isAfter(today))
                .map(RentSchedule::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = schedules.stream()
                .map(RentSchedule::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalDueToDate.subtract(totalPaid);

        LocalDateTime lastPaymentAt = schedules.stream()
                .filter(s -> s.getAmountPaid() != null && s.getAmountPaid().compareTo(BigDecimal.ZERO) > 0)
                .map(RentSchedule::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return buildResponse(lease.getTenantId(), lease, null, balance, null, lastPaymentAt);
    }

    // ------------------------------------------------------------------
    // Direct-unit balances — a persisted running ledger, since there's no
    // RentSchedule to derive one from.
    // ------------------------------------------------------------------

    public RentBalanceResponse getUnitBalance(String rentalUnitId, String tenantId) {
        RentBalance balance = rentBalanceRepository
                .findByRentalUnitIdAndTenantId(rentalUnitId, tenantId)
                .orElseGet(() -> {
                    RentBalance fresh = new RentBalance();
                    fresh.setRentalUnitId(rentalUnitId);
                    fresh.setTenantId(tenantId);
                    fresh.setBalance(BigDecimal.ZERO);
                    return fresh;
                });

        RentalUnit unit = rentalUnitRepository.findById(rentalUnitId).orElse(null);

        return buildResponse(tenantId, null, unit, balance.getBalance(),
                balance.getLastChargeAt(), balance.getLastPaymentAt());
    }

    @Transactional
    public RentBalance findOrCreateUnitBalance(String rentalUnitId, String tenantId, String propertyId) {
        return rentBalanceRepository.findByRentalUnitIdAndTenantId(rentalUnitId, tenantId)
                .orElseGet(() -> {
                    RentBalance balance = new RentBalance();
                    balance.setRentalUnitId(rentalUnitId);
                    balance.setTenantId(tenantId);
                    balance.setPropertyId(propertyId);
                    balance.setBalance(BigDecimal.ZERO);
                    return rentBalanceRepository.save(balance);
                });
    }

    /**
     * Catches the balance up to the current month by charging one month's
     * rent for each elapsed month since the last charge. On the very first
     * charge (no prior charge on file), a single month is charged — there's
     * no reliable way to know how long the unit has been occupied without
     * a schedule, so we start the clock from the first payment/charge event.
     */
    @Transactional
    public void chargeElapsedMonthsIfNeeded(RentBalance balance, BigDecimal monthlyRent) {
        LocalDateTime now = LocalDateTime.now();

        int monthsToCharge;
        if (balance.getLastChargeAt() == null) {
            monthsToCharge = 1;
        } else {
            YearMonth lastChargedMonth = YearMonth.from(balance.getLastChargeAt());
            YearMonth currentMonth = YearMonth.from(now);
            long elapsed = lastChargedMonth.until(currentMonth, ChronoUnit.MONTHS);
            monthsToCharge = (int) Math.min(Math.max(elapsed, 0), MAX_MONTHS_TO_BACK_CHARGE);
        }

        for (int i = 0; i < monthsToCharge; i++) {
            balance.setBalance(balance.getBalance().add(monthlyRent));
        }

        if (monthsToCharge > 0) {
            balance.setLastChargeAmount(monthlyRent);
            balance.setLastChargeAt(now);
        }
    }

    @Transactional
    public void applyPayment(RentBalance balance, BigDecimal amount) {
        balance.setBalance(balance.getBalance().subtract(amount));
        balance.setLastPaymentAmount(amount);
        balance.setLastPaymentAt(LocalDateTime.now());
        rentBalanceRepository.save(balance);
    }

    // Runs monthly so arrears accrue and are visible even if a tenant
    // hasn't made a payment recently — mirrors RentScheduleService's
    // markOverdueSchedules() cron, but for the direct-unit ledger.
    @Scheduled(cron = "0 0 6 1 * *")
    @Transactional
    public void chargeMonthlyRentForAllDirectUnits() {
        List<OccupancyHistory> activeDirectOccupancies =
                occupancyHistoryRepository.findByLeaseIdIsNullAndMoveOutDateIsNull();

        int charged = 0;
        int skipped = 0;

        for (OccupancyHistory occupancy : activeDirectOccupancies) {
            if (occupancy.getRentalUnitId() == null || occupancy.getTenantId() == null) {
                skipped++;
                continue;
            }

            RentalUnit unit = rentalUnitRepository.findById(occupancy.getRentalUnitId()).orElse(null);
            if (unit == null || unit.getRentAmount() == null) {
                log.warn("Skipping monthly rent charge for occupancy {} — rental unit {} not found or has "
                        + "no rentAmount configured", occupancy.getId(), occupancy.getRentalUnitId());
                skipped++;
                continue;
            }

            try {
                RentBalance balance = findOrCreateUnitBalance(
                        unit.getId(), occupancy.getTenantId(), unit.getPropertyId());
                chargeElapsedMonthsIfNeeded(balance, unit.getRentAmount());
                rentBalanceRepository.save(balance);
                charged++;
            } catch (Exception e) {
                log.error("Failed to apply monthly rent charge for unit {} / tenant {}: {}",
                        unit.getId(), occupancy.getTenantId(), e.getMessage());
                skipped++;
            }
        }

        log.info("Monthly direct-unit rent charge run complete: {} charged, {} skipped", charged, skipped);
    }

    // ------------------------------------------------------------------
    // Tenant-wide summary — aggregates every lease and every directly
    // occupied unit for a tenant into one "fee statement" view.
    // ------------------------------------------------------------------

    public TenantRentBalanceSummaryResponse getTenantSummary(String tenantId) {
        List<RentBalanceResponse> breakdown = new ArrayList<>();

        leaseRepository.findByTenantId(tenantId)
                .forEach(lease -> breakdown.add(getLeaseBalance(lease.getId())));

        occupancyHistoryRepository.findByTenantId(tenantId).stream()
                .filter(o -> o.getLeaseId() == null && o.getMoveOutDate() == null)
                .forEach(o -> breakdown.add(getUnitBalance(o.getRentalUnitId(), tenantId)));

        BigDecimal totalArrears = breakdown.stream()
                .map(RentBalanceResponse::getArrearsOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = breakdown.stream()
                .map(RentBalanceResponse::getCreditAvailable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TenantRentBalanceSummaryResponse summary = new TenantRentBalanceSummaryResponse();
        summary.setTenantId(tenantId);
        summary.setTotalArrearsOwed(totalArrears);
        summary.setTotalCreditAvailable(totalCredit);
        summary.setNetBalance(totalArrears.subtract(totalCredit));
        summary.setBreakdown(breakdown);
        tenantRepository.findById(tenantId).ifPresent(tenant -> summary.setTenant(toTenantSummary(tenant)));
        return summary;
    }

    // ------------------------------------------------------------------

    private RentBalanceResponse buildResponse(String tenantId, Lease lease, RentalUnit unit,
                                               BigDecimal balance, LocalDateTime lastChargeAt,
                                               LocalDateTime lastPaymentAt) {
        BigDecimal arrearsOwed = balance.compareTo(BigDecimal.ZERO) > 0 ? balance : BigDecimal.ZERO;
        BigDecimal creditAvailable = balance.compareTo(BigDecimal.ZERO) < 0 ? balance.negate() : BigDecimal.ZERO;

        String message;
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            message = "Fully settled — no arrears, no credit on file.";
        } else if (balance.compareTo(BigDecimal.ZERO) > 0) {
            message = "KES " + arrearsOwed + " in arrears.";
        } else {
            message = "KES " + creditAvailable + " in credit — will be applied automatically to future rent.";
        }

        RentBalanceResponse response = new RentBalanceResponse();
        response.setTenantId(tenantId);
        response.setLeaseId(lease != null ? lease.getId() : null);
        response.setRentalUnitId(unit != null ? unit.getId() : null);
        response.setBalance(balance);
        response.setArrearsOwed(arrearsOwed);
        response.setCreditAvailable(creditAvailable);
        response.setStatusMessage(message);
        response.setLastChargeAt(lastChargeAt);
        response.setLastPaymentAt(lastPaymentAt);

        enrichWithSummaries(response, tenantId, lease, unit);

        return response;
    }

    private void enrichWithSummaries(RentBalanceResponse response, String tenantId, Lease lease, RentalUnit unit) {
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }

        if (lease != null) {
            response.setLease(toLeaseSummary(lease));
        }

        // Resolve the unit: if the caller already fetched it (direct-unit
        // balance), use that. Otherwise, for a lease backed by a specific
        // unit (not a whole-property lease), fetch it here.
        RentalUnit resolvedUnit = unit;
        if (resolvedUnit == null && lease != null && lease.getRentalUnitId() != null) {
            resolvedUnit = rentalUnitRepository.findById(lease.getRentalUnitId()).orElse(null);
        }
        if (resolvedUnit != null) {
            response.setUnit(toRentalUnitSummary(resolvedUnit));
        }

        String propertyId = lease != null ? lease.getPropertyId() : null;
        if (propertyId == null && resolvedUnit != null) {
            propertyId = resolvedUnit.getPropertyId();
        }
        if (propertyId != null) {
            propertyRepository.findById(propertyId)
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }
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