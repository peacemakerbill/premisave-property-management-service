package com.premisave.property.service;

import com.premisave.property.dto.request.LeaseRentPaymentRequest;
import com.premisave.property.dto.request.SecurityDepositRequest;
import com.premisave.property.dto.response.LeaseRentPaymentResponse;
import com.premisave.property.dto.response.LeaseSummaryResponse;
import com.premisave.property.dto.response.PaymentDueResponse;
import com.premisave.property.dto.response.PropertySummaryResponse;
import com.premisave.property.dto.response.RentalUnitSummaryResponse;
import com.premisave.property.dto.response.TenantSummaryResponse;
import com.premisave.property.entity.Lease;
import com.premisave.property.entity.LeaseRentUnitPayment;
import com.premisave.property.entity.Property;
import com.premisave.property.entity.RentSchedule;
import com.premisave.property.entity.RentalUnit;
import com.premisave.property.entity.Tenant;
import com.premisave.property.entity.WalletTransfer;
import com.premisave.property.enums.PaymentMethod;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.enums.PaymentType;
import com.premisave.property.enums.WalletTransferPurpose;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.UnauthorizedException;
import com.premisave.property.repository.LeaseRentUnitPaymentRepository;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentScheduleRepository;
import com.premisave.property.repository.RentalUnitRepository;
import com.premisave.property.repository.SecurityDepositRepository;
import com.premisave.property.repository.TenantRepository;
import com.premisave.property.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseRentUnitPaymentService {

    // Safety cap on how many schedule entries a single payment can cascade
    // across (~2 years of monthly rent). Guards against a runaway loop if
    // schedule data is ever malformed (e.g. a zero-amount entry).
    private static final int MAX_SCHEDULES_TO_APPLY_PER_PAYMENT = 24;

    private static final List<PaymentStatus> OUTSTANDING_STATUSES =
            List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID, PaymentStatus.OVERDUE);

    private final LeaseRentUnitPaymentRepository leaseRentUnitPaymentRepository;
    private final RentScheduleRepository rentScheduleRepository;
    private final LeaseRepository leaseRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final PropertyRepository propertyRepository;
    private final SecurityDepositRepository securityDepositRepository;
    private final SecurityDepositService securityDepositService;
    private final WalletPaymentService walletPaymentService;
    private final TenantRepository tenantRepository;
    private final PaymentNotificationService paymentNotificationService;

    // ------------------------------------------------------------------
    // Payments are funded from the tenant's Premisave wallet: recordPayment()
    // moves the money tenant wallet -> property owner's wallet through
    // wallet-service (see WalletPaymentService) and only then books it
    // against the deposit / rent schedules. Wallets are topped up in
    // wallet-service (M-Pesa STK, Stripe, PayPal, ...), not here.
    // ------------------------------------------------------------------

    public PaymentDueResponse getPaymentDue(String leaseId) {
        Lease lease = findLeaseOrThrow(leaseId);

        boolean depositRequired;
        boolean depositHeld = securityDepositRepository.findByLeaseId(leaseId).isPresent();
        BigDecimal depositAmount;
        RentalUnit unit = null;

        if (lease.getRentalUnitId() != null) {
            unit = findUnitOrThrow(lease.getRentalUnitId());
            depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
            depositAmount = unit.getSecurityDeposit();
        } else {
            // Whole-property lease — deposit terms live directly on the Lease
            depositAmount = lease.getSecurityDeposit();
            depositRequired = depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        BigDecimal rentDue = rentScheduleRepository
                .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(leaseId, OUTSTANDING_STATUSES)
                .map(s -> s.getAmountDue().subtract(s.getAmountPaid()))
                .orElse(BigDecimal.ZERO);

        BigDecimal depositDue = (depositRequired && !depositHeld && depositAmount != null)
                ? depositAmount
                : BigDecimal.ZERO;

        PaymentDueResponse response = new PaymentDueResponse();
        response.setLeaseId(leaseId);
        response.setRentDue(rentDue);
        response.setDepositDue(depositDue);
        response.setTotalDue(rentDue.add(depositDue));
        response.setDepositRequired(depositRequired);
        response.setDepositAlreadyHeld(depositHeld);

        response.setLease(toLeaseSummary(lease));
        if (unit != null) {
            response.setUnit(toRentalUnitSummary(unit));
        }
        if (lease.getTenantId() != null) {
            tenantRepository.findById(lease.getTenantId())
                    .ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }
        if (lease.getPropertyId() != null) {
            propertyRepository.findById(lease.getPropertyId())
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }

        return response;
    }

    @Transactional
    public LeaseRentPaymentResponse recordPayment(LeaseRentPaymentRequest request, String tenantId) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }
        walletPaymentService.assertWalletPaymentMethod(request.getPaymentMethod());

        String reference = walletPaymentService.resolveReference(tenantId, request.getReference());

        // Idempotent replay: this reference was already paid and booked — return that payment,
        // don't touch the wallet or the rent schedule again.
        Optional<LeaseRentUnitPayment> replay = leaseRentUnitPaymentRepository.findByPaymentReference(reference);
        if (replay.isPresent()) {
            LeaseRentUnitPayment existing = replay.get();
            boolean sameRequest = request.getLeaseId().equals(existing.getLeaseId())
                    && tenantId.equals(existing.getTenantId())
                    && request.getAmount().compareTo(existing.getAmount()) == 0;
            if (!sameRequest) {
                throw new ConflictException("This payment reference was already used for a different payment");
            }
            return toResponse(existing);
        }

        Lease lease = findLeaseOrThrow(request.getLeaseId());

        if (!tenantId.equals(lease.getTenantId())) {
            throw new UnauthorizedException("You can only pay rent on your own lease");
        }

        boolean depositRequired;
        BigDecimal depositAmount;
        RentalUnit unit = null;

        if (lease.getRentalUnitId() != null) {
            unit = findUnitOrThrow(lease.getRentalUnitId());
            depositRequired = Boolean.TRUE.equals(unit.getDepositRequired());
            depositAmount = unit.getSecurityDeposit();
        } else {
            depositAmount = lease.getSecurityDeposit();
            depositRequired = depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        // ---- Validate everything BEFORE any money moves ----------------------------------------
        boolean depositAlreadyHeld = securityDepositRepository.findByLeaseId(lease.getId()).isPresent();

        BigDecimal depositToApply = BigDecimal.ZERO;
        if (depositRequired && !depositAlreadyHeld
                && depositAmount != null
                && depositAmount.compareTo(BigDecimal.ZERO) > 0) {

            if (request.getAmount().compareTo(depositAmount) < 0) {
                throw new BadRequestException(
                        "Payment of " + MoneyUtils.format(request.getAmount())
                                + " is less than the required security deposit of "
                                + MoneyUtils.format(depositAmount) + ". Deposit must be settled before or alongside rent.");
            }
            depositToApply = depositAmount;
        }

        BigDecimal rentPortion = request.getAmount().subtract(depositToApply);

        if (rentPortion.compareTo(BigDecimal.ZERO) > 0
                && rentScheduleRepository
                        .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(lease.getId(), OUTSTANDING_STATUSES)
                        .isEmpty()) {
            throw new ResourceNotFoundException("No outstanding rent due for this lease");
        }

        // Fetched once here so the description and both notifications below reuse the same lookup.
        Property property = propertyRepository.findById(lease.getPropertyId()).orElse(null);

        // ---- Move the money: tenant wallet -> property owner's wallet --------------------------
        WalletTransfer transfer = walletPaymentService.collect(new WalletPaymentService.CollectionCommand(
                reference, tenantId, lease.getPropertyId(), WalletTransferPurpose.LEASE_RENT, lease.getId(),
                request.getAmount(), "Rent payment for " + locationSummary(unit, property)));

        // ---- Book it ---------------------------------------------------------------------------
        LeaseBooking booking;
        try {
            booking = bookLeasePayment(lease.getId(), tenantId, request.getAmount(), transfer.getReference(),
                    depositToApply, rentPortion);
        } catch (RuntimeException e) {
            throw walletPaymentService.bookingFailed(transfer, e);
        }

        LeaseRentUnitPayment saved = booking.payment();
        RentApplicationResult rentResult = booking.rentResult();
        walletPaymentService.markBooked(transfer.getReference(), saved.getId());

        // Best-effort, asynchronous: tenant receipt email and an email to the property owner
        // whose wallet was credited. Never affects the payment that is already booked.
        paymentNotificationService.notifyRentPaid(new PaymentNotificationService.RentReceipt(
                tenantId, transfer.getOwnerId(), transfer.getRecipientAccount(),
                property != null ? property.getTitle() : null,
                unit != null ? unit.getUnitNumber() : null,
                saved.getAmountPaid(), saved.getDepositAmountApplied(), saved.getRentAmountApplied(),
                saved.getStatus(), saved.getPaymentReference(), saved.getPaidAt(),
                rentResult.applications().stream()
                        .map(a -> new PaymentNotificationService.PeriodLine(
                                a.dueDate(), a.amountApplied(), a.resultingStatus()))
                        .toList(),
                rentResult.finalStatus() == PaymentStatus.OVERPAID ? rentResult.finalOverpaidAmount() : null,
                null));

        return toResponse(saved);
    }

    public List<LeaseRentPaymentResponse> getPaymentHistory(String leaseId) {
        return leaseRentUnitPaymentRepository.findByLeaseId(leaseId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Local booking once the money has moved: hold the deposit portion, apply the
     * rent portion across schedule entries, and save the payment record.
     */
    private LeaseBooking bookLeasePayment(String leaseId, String tenantId, BigDecimal amount, String reference,
                                           BigDecimal depositToApply, BigDecimal rentPortion) {
        BigDecimal depositApplied = BigDecimal.ZERO;

        if (depositToApply.compareTo(BigDecimal.ZERO) > 0) {
            SecurityDepositRequest depositRequest = new SecurityDepositRequest();
            depositRequest.setLeaseId(leaseId);
            depositRequest.setAmount(depositToApply);
            securityDepositService.holdDeposit(depositRequest);
            depositApplied = depositToApply;
        }

        RentApplicationResult rentResult = applyRentAcrossSchedules(leaseId, rentPortion);

        LeaseRentUnitPayment payment = new LeaseRentUnitPayment();
        payment.setLeaseId(leaseId);
        payment.setTenantId(tenantId);
        payment.setAmount(amount);
        payment.setAmountPaid(amount);
        payment.setDepositAmountApplied(depositApplied);
        payment.setRentAmountApplied(rentResult.totalRentApplied());
        PaymentType paymentType = resolvePaymentType(depositApplied, rentResult.totalRentApplied());
        payment.setPaymentType(paymentType);
        payment.setPaymentMethod(PaymentMethod.WALLET);
        payment.setPaymentReference(reference);
        payment.setStatus(deriveTransactionStatus(rentResult));
        payment.setPaidAt(LocalDateTime.now());
        payment.setDescription(buildPaymentDescription(paymentType, depositApplied, rentResult));

        return new LeaseBooking(leaseRentUnitPaymentRepository.save(payment), rentResult);
    }

    // ------------------------------------------------------------------
    // Rent application across schedule entries — carries an overpayment
    // forward into the next due period(s) instead of capping at the first.
    // ------------------------------------------------------------------

    /**
     * Applies {@code amountToApply} against the lease's outstanding rent
     * schedule entries, in due-date order. If a payment fully settles the
     * current period with money left over, the excess is applied to the
     * next outstanding period, and so on. If there's nothing left to apply
     * it to (e.g. the lease has ended), the excess is held as a visible
     * OVERPAID credit on the last schedule entry touched, rather than
     * silently discarded.
     */
    private RentApplicationResult applyRentAcrossSchedules(String leaseId, BigDecimal amountToApply) {
        List<ScheduleApplication> applications = new ArrayList<>();

        if (amountToApply.compareTo(BigDecimal.ZERO) <= 0) {
            return new RentApplicationResult(BigDecimal.ZERO, null, applications, false, BigDecimal.ZERO);
        }

        BigDecimal remaining = amountToApply;
        BigDecimal totalApplied = BigDecimal.ZERO;
        PaymentStatus finalStatus = null;
        RentSchedule lastTouchedSchedule = null;
        BigDecimal finalOverpaidAmount = BigDecimal.ZERO;
        int iterations = 0;

        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            if (++iterations > MAX_SCHEDULES_TO_APPLY_PER_PAYMENT) {
                log.warn("Rent payment for lease {} touched more than {} schedule entries — stopping to avoid "
                                + "a runaway loop. Remaining {} will be credited as an overpayment on the "
                                + "last schedule entry touched.",
                        leaseId, MAX_SCHEDULES_TO_APPLY_PER_PAYMENT, MoneyUtils.format(remaining));
                break;
            }

            Optional<RentSchedule> nextScheduleOpt = rentScheduleRepository
                    .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(leaseId, OUTSTANDING_STATUSES);

            if (nextScheduleOpt.isEmpty()) {
                break; // nothing left to apply to — handled below
            }

            RentSchedule schedule = nextScheduleOpt.get();
            BigDecimal due = schedule.getAmountDue().subtract(schedule.getAmountPaid());

            if (remaining.compareTo(due) >= 0) {
                // Fully settles this period, possibly with room left for the next one.
                schedule.setAmountPaid(schedule.getAmountDue());
                schedule.setStatus(PaymentStatus.PAID);
                rentScheduleRepository.save(schedule);

                applications.add(new ScheduleApplication(schedule.getDueDate(), due, PaymentStatus.PAID));
                totalApplied = totalApplied.add(due);
                remaining = remaining.subtract(due);
                finalStatus = PaymentStatus.PAID;
                lastTouchedSchedule = schedule;
            } else {
                BigDecimal newAmountPaid = schedule.getAmountPaid().add(remaining);
                schedule.setAmountPaid(newAmountPaid);
                schedule.setStatus(PaymentStatus.PARTIALLY_PAID);
                rentScheduleRepository.save(schedule);

                applications.add(new ScheduleApplication(schedule.getDueDate(), remaining, PaymentStatus.PARTIALLY_PAID));
                totalApplied = totalApplied.add(remaining);
                finalStatus = PaymentStatus.PARTIALLY_PAID;
                lastTouchedSchedule = schedule;
                remaining = BigDecimal.ZERO;
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            if (lastTouchedSchedule == null) {
                // No outstanding schedule existed at all for this lease.
                throw new ResourceNotFoundException("No outstanding rent due for this lease");
            }

            // No further schedule entries exist — hold the excess as a
            // visible credit on the last entry touched.
            BigDecimal newAmountPaid = lastTouchedSchedule.getAmountPaid().add(remaining);
            lastTouchedSchedule.setAmountPaid(newAmountPaid);
            lastTouchedSchedule.setStatus(PaymentStatus.OVERPAID);
            rentScheduleRepository.save(lastTouchedSchedule);

            applications.add(new ScheduleApplication(lastTouchedSchedule.getDueDate(), remaining, PaymentStatus.OVERPAID));
            totalApplied = totalApplied.add(remaining);
            finalStatus = PaymentStatus.OVERPAID;
            finalOverpaidAmount = remaining;
        }

        boolean spilloverOccurred = applications.size() > 1 || finalStatus == PaymentStatus.OVERPAID;

        return new RentApplicationResult(totalApplied, finalStatus, applications, spilloverOccurred, finalOverpaidAmount);
    }

    /**
     * Human-readable "which property/unit is this about" fragment, used in
     * the wallet transfer description so a tenant on more than one lease/unit
     * can tell them apart at a glance.
     * Falls back gracefully if property or unit lookups came back empty.
     */
    private String locationSummary(RentalUnit unit, Property property) {
        String propertyName = (property != null && property.getTitle() != null && !property.getTitle().isBlank())
                ? property.getTitle() : "your property";

        if (unit != null && unit.getUnitNumber() != null && !unit.getUnitNumber().isBlank()) {
            return propertyName + ", Unit " + unit.getUnitNumber();
        }
        return propertyName;
    }

    private PaymentType resolvePaymentType(BigDecimal depositApplied, BigDecimal rentApplied) {
        boolean hasDeposit = depositApplied.compareTo(BigDecimal.ZERO) > 0;
        boolean hasRent = rentApplied.compareTo(BigDecimal.ZERO) > 0;
        if (hasDeposit && hasRent) return PaymentType.RENT_AND_DEPOSIT;
        if (hasDeposit) return PaymentType.SECURITY_DEPOSIT;
        return PaymentType.RENT;
    }

    /**
     * The transaction-level outcome, as the payer experienced it — distinct
     * from the status of whichever schedule entry the loop happened to
     * finish on. If the payment touched more than one schedule entry, it
     * necessarily paid more than what was strictly due at the time (the
     * excess spilled forward, or was held as a credit) — that's an
     * OVERPAID transaction from the payer's point of view, even if the
     * final period it reached only ended up partially covered. Only a
     * payment that touched exactly one entry and fell short of it is a
     * genuine PARTIALLY_PAID transaction.
     */
    private PaymentStatus deriveTransactionStatus(RentApplicationResult rentResult) {
        List<ScheduleApplication> applications = rentResult.applications();

        if (applications.isEmpty()) {
            return PaymentStatus.PAID; // pure deposit payment, no rent portion
        }
        if (applications.size() > 1) {
            return PaymentStatus.OVERPAID;
        }
        return rentResult.finalStatus();
    }

    /**
     * Builds a plain-language, point-in-time summary of what this specific
     * transaction did — including a breakdown when the payment spilled
     * across multiple billing periods, and flagging any overpayment credit.
     * This is a snapshot, written once; for a schedule entry's current live
     * balance, see RentScheduleResponse instead.
     */
    private String buildPaymentDescription(PaymentType paymentType, BigDecimal depositApplied,
                                            RentApplicationResult rentResult) {
        StringBuilder message = new StringBuilder();

        if (depositApplied.compareTo(BigDecimal.ZERO) > 0) {
            message.append(MoneyUtils.format(depositApplied)).append(" applied to your security deposit. ");
        }

        List<ScheduleApplication> applications = rentResult.applications();

        if (applications.isEmpty()) {
            if (paymentType == PaymentType.SECURITY_DEPOSIT) {
                message.append("No outstanding rent was covered by this payment.");
            }
            return message.toString().trim();
        }

        if (applications.size() == 1) {
            ScheduleApplication app = applications.get(0);
            message.append(switch (app.resultingStatus()) {
                case PAID -> "Rent payment of " + MoneyUtils.format(app.amountApplied())
                        + " received in full for the period due " + app.dueDate() + ".";
                case PARTIALLY_PAID -> "Partial rent payment of " + MoneyUtils.format(app.amountApplied())
                        + " received for the period due " + app.dueDate() + ".";
                case OVERPAID -> "Rent payment received with an overpayment of "
                        + MoneyUtils.format(app.amountApplied()) + " credited above the amount due for the period due "
                        + app.dueDate() + ". Please contact your property owner regarding a credit or refund.";
                default -> "Rent payment of " + MoneyUtils.format(app.amountApplied())
                        + " recorded for the period due " + app.dueDate() + ".";
            });
        } else {
            message.append("Rent payment of ").append(MoneyUtils.format(rentResult.totalRentApplied()))
                    .append(" applied across ").append(applications.size()).append(" billing periods: ");

            String breakdown = applications.stream()
                    .map(app -> switch (app.resultingStatus()) {
                        case PAID -> app.dueDate() + " (paid in full, " + MoneyUtils.format(app.amountApplied()) + ")";
                        case PARTIALLY_PAID -> app.dueDate() + " (partially paid, "
                                + MoneyUtils.format(app.amountApplied()) + ")";
                        case OVERPAID -> app.dueDate() + " (overpaid by " + MoneyUtils.format(app.amountApplied()) + ")";
                        default -> app.dueDate() + " (" + MoneyUtils.format(app.amountApplied()) + ")";
                    })
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("");

            message.append(breakdown).append(".");

            if (rentResult.finalStatus() == PaymentStatus.OVERPAID) {
                message.append(" The excess of ").append(MoneyUtils.format(rentResult.finalOverpaidAmount()))
                        .append(" has been credited as an overpayment. Please contact your property owner ")
                        .append("regarding a refund or credit toward a future bill.");
            } else {
                message.append(" The extra amount was automatically applied toward your upcoming rent.");
            }
        }

        return message.toString().trim();
    }

    private Lease findLeaseOrThrow(String leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
    }

    private RentalUnit findUnitOrThrow(String unitId) {
        return rentalUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental unit not found"));
    }

    private LeaseRentPaymentResponse toResponse(LeaseRentUnitPayment payment) {
        LeaseRentPaymentResponse response = new LeaseRentPaymentResponse();
        response.setId(payment.getId());
        response.setLeaseId(payment.getLeaseId());
        response.setPaymentType(payment.getPaymentType());
        response.setAmount(payment.getAmountPaid());
        response.setRentAmountApplied(payment.getRentAmountApplied());
        response.setDepositAmountApplied(payment.getDepositAmountApplied());
        response.setStatus(payment.getStatus().name());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentReference(payment.getPaymentReference());
        response.setPaidAt(payment.getPaidAt());
        response.setDescription(payment.getDescription());

        if (payment.getTenantId() != null) {
            tenantRepository.findById(payment.getTenantId())
                    .ifPresent(tenant -> response.setTenant(toTenantSummary(tenant)));
        }

        if (payment.getLeaseId() != null) {
            leaseRepository.findById(payment.getLeaseId()).ifPresent(lease -> {
                response.setLease(toLeaseSummary(lease));
                enrichWithPropertyAndUnit(response, lease);
            });
        }

        return response;
    }

    private void enrichWithPropertyAndUnit(LeaseRentPaymentResponse response, Lease lease) {
        if (lease.getPropertyId() != null) {
            propertyRepository.findById(lease.getPropertyId())
                    .ifPresent(property -> response.setProperty(toPropertySummary(property)));
        }
        if (lease.getRentalUnitId() != null) {
            rentalUnitRepository.findById(lease.getRentalUnitId())
                    .ifPresent(unit -> response.setUnit(toRentalUnitSummary(unit)));
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

    // ------------------------------------------------------------------
    // Internal result types
    // ------------------------------------------------------------------

    private record ScheduleApplication(LocalDate dueDate, BigDecimal amountApplied, PaymentStatus resultingStatus) {
    }

    private record RentApplicationResult(
            BigDecimal totalRentApplied,
            PaymentStatus finalStatus,
            List<ScheduleApplication> applications,
            boolean spilloverOccurred,
            BigDecimal finalOverpaidAmount) {
    }

    private record LeaseBooking(LeaseRentUnitPayment payment, RentApplicationResult rentResult) {
    }
}