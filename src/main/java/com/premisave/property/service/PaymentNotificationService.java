package com.premisave.property.service;

import com.premisave.property.email.EmailContent;
import com.premisave.property.email.EmailContent.Callout;
import com.premisave.property.email.EmailContent.Row;
import com.premisave.property.email.EmailContent.Section;
import com.premisave.property.email.EmailTone;
import com.premisave.property.entity.Owner;
import com.premisave.property.entity.Tenant;
import com.premisave.property.enums.PaymentStatus;
import com.premisave.property.repository.OwnerRepository;
import com.premisave.property.repository.TenantRepository;
import com.premisave.property.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sends the emails (and short SMS) for everything money-related: rent payments,
 * security deposits, utility bills and failed payments.
 *
 * Every public method is @Async on the shared "taskExecutor" pool, so callers return
 * immediately, and every method swallows its own failures — a notification problem must
 * never affect a payment that is already booked. Callers pass plain values (no entities),
 * so nothing here depends on the caller's transaction having committed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

    private final EmailService emailService;
    private final SmsService smsService;
    private final TenantRepository tenantRepository;
    private final OwnerRepository ownerRepository;

    @Value("${frontend.url:http://localhost:3000}")
    private String appUrl;

    // ------------------------------------------------------------------
    // Notification payloads (plain values, built by the calling service)
    // ------------------------------------------------------------------

    /** One billing period a rent payment was applied to (lease payments only). */
    public record PeriodLine(LocalDate dueDate, BigDecimal amount, PaymentStatus status) {
    }

    /**
     * @param ownerEmail         the owner's wallet/account email (where the money went)
     * @param creditAmount       overpayment held as credit, or null
     * @param outstandingBalance arrears still owed after this payment (direct-unit only), or null
     */
    public record RentReceipt(String tenantId, String ownerId, String ownerEmail,
                              String propertyTitle, String unitNumber,
                              BigDecimal amount, BigDecimal depositApplied, BigDecimal rentApplied,
                              PaymentStatus status, String reference, LocalDateTime paidAt,
                              List<PeriodLine> periods, BigDecimal creditAmount, BigDecimal outstandingBalance) {
    }

    public record UtilityBillNotice(String tenantId, String utilityType, String propertyTitle, String unitNumber,
                                    BigDecimal amount, LocalDateTime periodStart, LocalDateTime periodEnd,
                                    String billId, BigDecimal previousReading, BigDecimal currentReading,
                                    BigDecimal consumption, BigDecimal ratePerUnit) {
    }

    public record UtilityPaymentReceipt(String tenantId, String ownerId, String ownerEmail, String utilityType,
                                        String propertyTitle, String unitNumber, BigDecimal amountPaid,
                                        BigDecimal billAmount, BigDecimal totalPaid, BigDecimal balanceDue,
                                        PaymentStatus status, String reference, LocalDateTime paidAt) {
    }

    public record DepositReceipt(String tenantId, String propertyTitle, String unitNumber,
                                 BigDecimal amount, LocalDateTime at) {
    }

    public record RefundLine(LocalDateTime at, BigDecimal amount, String reason) {
    }

    public record DepositRefundReceipt(String tenantId, String propertyTitle, String unitNumber,
                                       BigDecimal refundedNow, String reason, boolean finalRefund,
                                       BigDecimal depositAmount, BigDecimal totalRefunded, BigDecimal remaining,
                                       List<RefundLine> history, LocalDateTime at) {
    }

    // ------------------------------------------------------------------
    // Rent payments — tenant receipt + owner notification
    // ------------------------------------------------------------------

    @Async("taskExecutor")
    public void notifyRentPaid(RentReceipt r) {
        try {
            Tenant tenant = tenantRepository.findById(r.tenantId()).orElse(null);
            String location = location(r.propertyTitle(), r.unitNumber());

            if (tenant == null) {
                log.warn("Skipping rent receipt — tenant {} not found", r.tenantId());
            } else {
                boolean emailSent = emailService.send(tenant.getEmail(),
                        "Payment received — " + MoneyUtils.format(r.amount()), tenantReceipt(tenant, r, location));

                String sms = "Premisave: payment of " + MoneyUtils.format(r.amount()) + " received for " + location + ".";
                if (positive(r.creditAmount())) {
                    sms += " " + MoneyUtils.format(r.creditAmount()) + " is held as credit.";
                } else if (positive(r.outstandingBalance())) {
                    sms += " " + MoneyUtils.format(r.outstandingBalance()) + " still outstanding.";
                }
                boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(), sms);

                log.info("Rent receipt for {} sent (emailSent={}, smsSent={})", r.reference(), emailSent, smsSent);
            }

            notifyOwner(r.ownerId(), r.ownerEmail(), "Rent received — " + MoneyUtils.format(r.amount()),
                    ownerName -> ownerRentNotice(ownerName, tenant, r, location));
        } catch (Exception e) {
            log.error("Failed to send rent payment notifications for {}: {}", r.reference(), e.getMessage());
        }
    }

    private EmailContent tenantReceipt(Tenant tenant, RentReceipt r, String location) {
        List<Row> details = new ArrayList<>();
        details.add(Row.of("Date", dateTime(r.paidAt())));
        details.add(Row.of("Property", hasText(r.propertyTitle()) ? r.propertyTitle() : "—"));
        if (hasText(r.unitNumber())) {
            details.add(Row.of("Unit", r.unitNumber()));
        }
        details.add(Row.of("Paid from", "Premisave wallet"));
        details.add(Row.of("Reference", r.reference()));

        List<Row> applied = appliedRows(r);

        EmailContent.EmailContentBuilder builder = EmailContent.builder()
                .tone(EmailTone.SUCCESS)
                .preheader("We've received " + MoneyUtils.format(r.amount()) + " for " + location + ".")
                .title(r.status() == PaymentStatus.PARTIALLY_PAID ? "Partial payment received" : "Payment received")
                .subtitle(location)
                .greetingName(firstName(tenant.getFullName()))
                .paragraph("Thank you — we've received your payment and applied it to your account. "
                        + "Please keep this email as your receipt.")
                .amountLabel("Amount paid")
                .amount(MoneyUtils.amount(r.amount()))
                .currency(MoneyUtils.CURRENCY)
                .section(Section.of("Payment details", details));

        if (!applied.isEmpty()) {
            builder.section(Section.of("How it was applied", applied));
        }

        if (positive(r.creditAmount())) {
            builder.callout(new Callout(EmailTone.INFO, "Credit on your account",
                    "You've paid " + MoneyUtils.format(r.creditAmount()) + " more than is currently due. It stays on "
                            + "your account as credit — contact your property owner if you'd like a refund or to "
                            + "apply it to a future bill."));
        } else if (r.periods() != null && r.periods().size() > 1) {
            builder.callout(new Callout(EmailTone.INFO, "Applied to upcoming rent",
                    "Your payment covered more than one billing period, so the extra was applied to your "
                            + "upcoming rent (see the breakdown above)."));
        } else if (positive(r.outstandingBalance())) {
            builder.callout(new Callout(EmailTone.WARNING, "Balance remaining",
                    MoneyUtils.format(r.outstandingBalance()) + " is still outstanding on this account."));
        }

        return builder
                .ctaLabel("View my payments")
                .ctaUrl(appUrl)
                .footnote("Payments are made from your Premisave wallet. You can see every transaction in your wallet history.")
                .build();
    }

    private EmailContent ownerRentNotice(String ownerName, Tenant tenant, RentReceipt r, String location) {
        String tenantName = tenant != null && hasText(tenant.getFullName()) ? tenant.getFullName() : "A tenant";

        List<Row> details = new ArrayList<>();
        details.add(Row.of("Tenant", tenantName));
        if (tenant != null && hasText(tenant.getPhoneNumber())) {
            details.add(Row.of("Phone", tenant.getPhoneNumber()));
        }
        details.add(Row.of("Property", hasText(r.propertyTitle()) ? r.propertyTitle() : "—"));
        if (hasText(r.unitNumber())) {
            details.add(Row.of("Unit", r.unitNumber()));
        }
        details.add(Row.of("Date", dateTime(r.paidAt())));
        details.add(Row.of("Reference", r.reference()));

        EmailContent.EmailContentBuilder builder = EmailContent.builder()
                .tone(EmailTone.SUCCESS)
                .preheader(tenantName + " paid " + MoneyUtils.format(r.amount()) + " for " + location + ".")
                .title("Rent payment received")
                .subtitle(location)
                .greetingName(firstName(ownerName))
                .paragraph(tenantName + " has paid for " + location + ". The funds have been sent to your Premisave wallet.")
                .amountLabel("Payment from tenant")
                .amount(MoneyUtils.amount(r.amount()))
                .currency(MoneyUtils.CURRENCY)
                .section(Section.of("Payment details", details));

        List<Row> applied = appliedRows(r);
        if (!applied.isEmpty()) {
            builder.section(Section.of("How it was applied", applied));
        }
        if (positive(r.outstandingBalance())) {
            builder.callout(new Callout(EmailTone.WARNING, "Balance still owed",
                    tenantName + " still owes " + MoneyUtils.format(r.outstandingBalance()) + " on this unit."));
        }

        return builder
                .ctaLabel("Open Premisave")
                .ctaUrl(appUrl)
                .footnote("Any wallet fees are shown in your wallet transaction history.")
                .build();
    }

    private List<Row> appliedRows(RentReceipt r) {
        List<Row> rows = new ArrayList<>();
        if (positive(r.depositApplied())) {
            rows.add(Row.of("Security deposit", MoneyUtils.format(r.depositApplied())));
        }
        if (r.periods() != null && !r.periods().isEmpty()) {
            for (PeriodLine p : r.periods()) {
                rows.add(Row.withBadge("Rent due " + DATE.format(p.dueDate()), MoneyUtils.format(p.amount()),
                        periodBadge(p.status()), badgeTone(p.status())));
            }
        } else if (positive(r.rentApplied())) {
            rows.add(Row.of("Rent", MoneyUtils.format(r.rentApplied())));
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // Utility bills — issued, and paid
    // ------------------------------------------------------------------

    @Async("taskExecutor")
    public void notifyUtilityBillIssued(UtilityBillNotice n) {
        try {
            Tenant tenant = tenantRepository.findById(n.tenantId()).orElse(null);
            if (tenant == null) {
                log.warn("Skipping utility bill notice — tenant {} not found", n.tenantId());
                return;
            }
            String location = location(n.propertyTitle(), n.unitNumber());
            String utility = pretty(n.utilityType());

            List<Row> details = new ArrayList<>();
            details.add(Row.of("Utility", utility));
            details.add(Row.of("Property", hasText(n.propertyTitle()) ? n.propertyTitle() : "—"));
            if (hasText(n.unitNumber())) {
                details.add(Row.of("Unit", n.unitNumber()));
            }
            String period = billingPeriod(n.periodStart(), n.periodEnd());
            if (period != null) {
                details.add(Row.of("Billing period", period));
            }

            EmailContent.EmailContentBuilder builder = EmailContent.builder()
                    .tone(EmailTone.INFO)
                    .preheader("A new " + utility.toLowerCase() + " bill of " + MoneyUtils.format(n.amount())
                            + " has been issued for " + location + ".")
                    .title("New utility bill")
                    .subtitle(utility + " — " + location)
                    .greetingName(firstName(tenant.getFullName()))
                    .paragraph("A new " + utility.toLowerCase() + " bill has been issued for " + location
                            + ". You can pay it from your Premisave wallet.")
                    .amountLabel("Amount due")
                    .amount(MoneyUtils.amount(n.amount()))
                    .currency(MoneyUtils.CURRENCY)
                    .section(Section.of("Bill details", details));

            if (n.consumption() != null) {
                List<Row> meter = new ArrayList<>();
                if (n.previousReading() != null) {
                    meter.add(Row.of("Previous reading", plain(n.previousReading())));
                }
                if (n.currentReading() != null) {
                    meter.add(Row.of("Current reading", plain(n.currentReading())));
                }
                meter.add(Row.of("Consumption", plain(n.consumption()) + " units"));
                if (n.ratePerUnit() != null) {
                    meter.add(Row.of("Rate per unit", MoneyUtils.format(n.ratePerUnit())));
                }
                builder.section(Section.of("Meter reading", meter));
            }

            boolean emailSent = emailService.send(tenant.getEmail(),
                    "New " + utility.toLowerCase() + " bill — " + MoneyUtils.format(n.amount()),
                    builder.ctaLabel("Pay this bill")
                            .ctaUrl(appUrl)
                            .footnote("Bill reference: " + n.billId())
                            .build());

            boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(),
                    "Premisave: new " + utility.toLowerCase() + " bill of " + MoneyUtils.format(n.amount())
                            + " for " + location + ". Pay it from your wallet.");

            log.info("Utility bill notice for bill {} sent (emailSent={}, smsSent={})", n.billId(), emailSent, smsSent);
        } catch (Exception e) {
            log.error("Failed to send utility bill notice for bill {}: {}", n.billId(), e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void notifyUtilityBillPaid(UtilityPaymentReceipt r) {
        try {
            Tenant tenant = tenantRepository.findById(r.tenantId()).orElse(null);
            String location = location(r.propertyTitle(), r.unitNumber());
            String utility = pretty(r.utilityType());

            if (tenant == null) {
                log.warn("Skipping utility payment receipt — tenant {} not found", r.tenantId());
            } else {
                boolean emailSent = emailService.send(tenant.getEmail(),
                        "Utility payment received — " + MoneyUtils.format(r.amountPaid()),
                        utilityTenantReceipt(tenant, r, location, utility));
                boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(),
                        "Premisave: payment of " + MoneyUtils.format(r.amountPaid()) + " received for your "
                                + utility.toLowerCase() + " bill (" + location + ")."
                                + (positive(r.balanceDue()) ? " " + MoneyUtils.format(r.balanceDue()) + " still due." : ""));
                log.info("Utility payment receipt for {} sent (emailSent={}, smsSent={})", r.reference(), emailSent, smsSent);
            }

            notifyOwner(r.ownerId(), r.ownerEmail(), "Utility payment received — " + MoneyUtils.format(r.amountPaid()),
                    ownerName -> utilityOwnerNotice(ownerName, tenant, r, location, utility));
        } catch (Exception e) {
            log.error("Failed to send utility payment notifications for {}: {}", r.reference(), e.getMessage());
        }
    }

    private EmailContent utilityTenantReceipt(Tenant tenant, UtilityPaymentReceipt r, String location, String utility) {
        boolean settled = r.status() == PaymentStatus.PAID || r.status() == PaymentStatus.OVERPAID;

        EmailContent.EmailContentBuilder builder = EmailContent.builder()
                .tone(EmailTone.SUCCESS)
                .preheader("We've received " + MoneyUtils.format(r.amountPaid()) + " for your " + utility.toLowerCase() + " bill.")
                .title(settled ? "Utility bill paid" : "Partial payment received")
                .subtitle(utility + " — " + location)
                .greetingName(firstName(tenant.getFullName()))
                .paragraph("Thank you — your " + utility.toLowerCase() + " payment has been received. "
                        + "Please keep this email as your receipt.")
                .amountLabel("Amount paid")
                .amount(MoneyUtils.amount(r.amountPaid()))
                .currency(MoneyUtils.CURRENCY)
                .section(Section.of("Payment details", utilityRows(r, true)));

        if (positive(r.balanceDue()) && !settled) {
            builder.callout(new Callout(EmailTone.WARNING, "Balance remaining",
                    MoneyUtils.format(r.balanceDue()) + " is still outstanding on this bill."));
        } else if (r.status() == PaymentStatus.OVERPAID) {
            BigDecimal over = r.totalPaid().subtract(r.billAmount());
            builder.callout(new Callout(EmailTone.INFO, "Credit on this bill",
                    "You've paid " + MoneyUtils.format(over) + " more than this bill. Contact your property owner "
                            + "about a refund or credit toward a future bill."));
        }

        return builder.ctaLabel("View my bills").ctaUrl(appUrl)
                .footnote("Payments are made from your Premisave wallet.").build();
    }

    private EmailContent utilityOwnerNotice(String ownerName, Tenant tenant, UtilityPaymentReceipt r,
                                             String location, String utility) {
        String tenantName = tenant != null && hasText(tenant.getFullName()) ? tenant.getFullName() : "A tenant";

        EmailContent.EmailContentBuilder builder = EmailContent.builder()
                .tone(EmailTone.SUCCESS)
                .preheader(tenantName + " paid " + MoneyUtils.format(r.amountPaid()) + " toward a " + utility.toLowerCase() + " bill.")
                .title("Utility payment received")
                .subtitle(utility + " — " + location)
                .greetingName(firstName(ownerName))
                .paragraph(tenantName + " has paid toward the " + utility.toLowerCase() + " bill for " + location
                        + ". The funds have been sent to your Premisave wallet.")
                .amountLabel("Payment from tenant")
                .amount(MoneyUtils.amount(r.amountPaid()))
                .currency(MoneyUtils.CURRENCY)
                .section(Section.of("Payment details", utilityRows(r, false)));

        if (positive(r.balanceDue())) {
            builder.callout(new Callout(EmailTone.WARNING, "Balance still owed",
                    MoneyUtils.format(r.balanceDue()) + " remains outstanding on this bill."));
        }
        return builder.ctaLabel("Open Premisave").ctaUrl(appUrl)
                .footnote("Any wallet fees are shown in your wallet transaction history.").build();
    }

    private List<Row> utilityRows(UtilityPaymentReceipt r, boolean forTenant) {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("Date", dateTime(r.paidAt())));
        rows.add(Row.of("Bill total", MoneyUtils.format(r.billAmount())));
        rows.add(Row.of("Paid so far", MoneyUtils.format(r.totalPaid())));
        rows.add(positive(r.balanceDue())
                ? Row.withBadge("Balance due", MoneyUtils.format(r.balanceDue()), "Open", EmailTone.WARNING)
                : Row.withBadge("Balance due", MoneyUtils.format(BigDecimal.ZERO), "Settled", EmailTone.SUCCESS));
        if (forTenant) {
            rows.add(Row.of("Paid from", "Premisave wallet"));
        }
        rows.add(Row.of("Reference", r.reference()));
        return rows;
    }

    // ------------------------------------------------------------------
    // Failed payment (wallet-service rejected the transfer; nothing moved)
    // ------------------------------------------------------------------

    @Async("taskExecutor")
    public void notifyPaymentFailed(String tenantId, String description, BigDecimal amount,
                                    String reason, String retryReference) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) {
                log.warn("Skipping failed-payment notice — tenant {} not found", tenantId);
                return;
            }
            String why = hasText(reason) ? reason : "The payment could not be completed.";

            List<Row> details = new ArrayList<>();
            if (hasText(description)) {
                details.add(Row.of("For", description));
            }
            details.add(Row.of("Date", dateTime(null)));
            if (hasText(retryReference)) {
                details.add(Row.of("Reference", retryReference));
            }

            EmailContent content = EmailContent.builder()
                    .tone(EmailTone.DANGER)
                    .preheader("Your payment of " + MoneyUtils.format(amount) + " was not completed. No money was taken.")
                    .title("Payment unsuccessful")
                    .subtitle("We couldn't complete your payment")
                    .greetingName(firstName(tenant.getFullName()))
                    .paragraph("Your payment didn't go through. No money has left your wallet — your balance is unaffected.")
                    .amountLabel("Amount")
                    .amount(MoneyUtils.amount(amount))
                    .currency(MoneyUtils.CURRENCY)
                    .section(Section.of("Details", details))
                    .callout(new Callout(EmailTone.DANGER, "Reason", why))
                    .ctaLabel("Try again")
                    .ctaUrl(appUrl)
                    .footnote("If your wallet balance is low, top it up first and then retry the payment.")
                    .build();

            boolean emailSent = emailService.send(tenant.getEmail(), "Payment unsuccessful — " + MoneyUtils.format(amount), content);
            boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(),
                    "Premisave: your payment of " + MoneyUtils.format(amount) + " was not completed. " + why
                            + " No money was taken.");
            log.info("Failed-payment notice for tenant {} sent (emailSent={}, smsSent={})", tenantId, emailSent, smsSent);
        } catch (Exception e) {
            log.error("Failed to send failed-payment notice to tenant {}: {}", tenantId, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Security deposits
    // ------------------------------------------------------------------

    @Async("taskExecutor")
    public void notifyDepositHeld(DepositReceipt d) {
        try {
            Tenant tenant = tenantRepository.findById(d.tenantId()).orElse(null);
            if (tenant == null) {
                log.warn("Skipping deposit-held notice — tenant {} not found", d.tenantId());
                return;
            }
            String location = location(d.propertyTitle(), d.unitNumber());

            List<Row> details = new ArrayList<>();
            details.add(Row.of("Date", dateTime(d.at())));
            details.add(Row.of("Property", hasText(d.propertyTitle()) ? d.propertyTitle() : "—"));
            if (hasText(d.unitNumber())) {
                details.add(Row.of("Unit", d.unitNumber()));
            }
            details.add(Row.withBadge("Status", "Deposit held", "Held", EmailTone.INFO));

            EmailContent content = EmailContent.builder()
                    .tone(EmailTone.SUCCESS)
                    .preheader("Your security deposit of " + MoneyUtils.format(d.amount()) + " for " + location + " is on record.")
                    .title("Security deposit received")
                    .subtitle(location)
                    .greetingName(firstName(tenant.getFullName()))
                    .paragraph("We've recorded your security deposit for " + location + ".")
                    .amountLabel("Deposit held")
                    .amount(MoneyUtils.amount(d.amount()))
                    .currency(MoneyUtils.CURRENCY)
                    .section(Section.of("Deposit details", details))
                    .callout(new Callout(EmailTone.INFO, "When is it refunded?",
                            "Your deposit is refunded, in full or in part, when your tenancy ends, subject to the "
                                    + "condition of the property."))
                    .ctaLabel("View my deposit")
                    .ctaUrl(appUrl)
                    .build();

            boolean emailSent = emailService.send(tenant.getEmail(), "Security deposit received — " + MoneyUtils.format(d.amount()), content);
            boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(),
                    "Premisave: your security deposit of " + MoneyUtils.format(d.amount()) + " for " + location
                            + " has been recorded and held.");
            log.info("Deposit-held notice for tenant {} sent (emailSent={}, smsSent={})", d.tenantId(), emailSent, smsSent);
        } catch (Exception e) {
            log.error("Failed to send deposit-held notice to tenant {}: {}", d.tenantId(), e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void notifyDepositRefund(DepositRefundReceipt d) {
        try {
            Tenant tenant = tenantRepository.findById(d.tenantId()).orElse(null);
            if (tenant == null) {
                log.warn("Skipping deposit-refund notice — tenant {} not found", d.tenantId());
                return;
            }
            String location = location(d.propertyTitle(), d.unitNumber());

            List<Row> summary = new ArrayList<>();
            summary.add(Row.of("Date", dateTime(d.at())));
            summary.add(Row.of("Deposit held", MoneyUtils.format(d.depositAmount())));
            summary.add(Row.of("Refunded to date", MoneyUtils.format(d.totalRefunded())));
            summary.add(d.finalRefund()
                    ? Row.withBadge("Remaining", MoneyUtils.format(BigDecimal.ZERO), "Closed", EmailTone.SUCCESS)
                    : Row.withBadge("Remaining", MoneyUtils.format(d.remaining()), "Held", EmailTone.INFO));
            if (hasText(d.reason())) {
                summary.add(Row.of("Reason", d.reason()));
            }

            EmailContent.EmailContentBuilder builder = EmailContent.builder()
                    .tone(d.finalRefund() ? EmailTone.SUCCESS : EmailTone.INFO)
                    .preheader("A refund of " + MoneyUtils.format(d.refundedNow()) + " was recorded against your deposit for " + location + ".")
                    .title(d.finalRefund() ? "Security deposit fully refunded" : "Security deposit partially refunded")
                    .subtitle(location)
                    .greetingName(firstName(tenant.getFullName()))
                    .paragraph("A refund has been recorded against your security deposit for " + location + ".")
                    .amountLabel("Refunded now")
                    .amount(MoneyUtils.amount(d.refundedNow()))
                    .currency(MoneyUtils.CURRENCY)
                    .section(Section.of("Refund summary", summary));

            if (d.history() != null && d.history().size() > 1) {
                List<Row> history = new ArrayList<>();
                for (RefundLine line : d.history()) {
                    history.add(Row.of(dateTime(line.at()), MoneyUtils.format(line.amount())));
                }
                builder.section(Section.of("Refund history", history));
            }

            builder.callout(d.finalRefund()
                    ? new Callout(EmailTone.SUCCESS, "Deposit closed", "Your deposit has been fully refunded — nothing remains held.")
                    : new Callout(EmailTone.INFO, "Remaining balance",
                            MoneyUtils.format(d.remaining()) + " is still held against your deposit."));

            boolean emailSent = emailService.send(tenant.getEmail(),
                    (d.finalRefund() ? "Security deposit fully refunded — " : "Security deposit partially refunded — ")
                            + MoneyUtils.format(d.refundedNow()),
                    builder.ctaLabel("View my deposit").ctaUrl(appUrl).build());

            String sms = "Premisave: " + MoneyUtils.format(d.refundedNow()) + " refunded from your deposit (" + location + ").";
            if (!d.finalRefund()) {
                sms += " Remaining: " + MoneyUtils.format(d.remaining()) + ".";
            }
            boolean smsSent = smsService.sendNoticeSms(tenant.getPhoneNumber(), sms);
            log.info("Deposit-refund notice for tenant {} sent (emailSent={}, smsSent={})", d.tenantId(), emailSent, smsSent);
        } catch (Exception e) {
            log.error("Failed to send deposit-refund notice to tenant {}: {}", d.tenantId(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Sends the owner-side email, if we know where to send it. */
    private void notifyOwner(String ownerId, String ownerEmail, String subject,
                             java.util.function.Function<String, EmailContent> contentForOwnerName) {
        if (!hasText(ownerEmail)) {
            log.warn("Skipping owner notification '{}' — no owner email", subject);
            return;
        }
        String ownerName = ownerId != null
                ? ownerRepository.findById(ownerId).map(Owner::getFullName).orElse(null)
                : null;
        boolean sent = emailService.send(ownerEmail, subject, contentForOwnerName.apply(ownerName));
        log.info("Owner notification '{}' sent={}", subject, sent);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String location(String propertyTitle, String unitNumber) {
        String property = hasText(propertyTitle) ? propertyTitle : "your property";
        return hasText(unitNumber) ? property + ", Unit " + unitNumber : property;
    }

    private static String firstName(String fullName) {
        if (!hasText(fullName)) {
            return null;
        }
        return fullName.trim().split("\\s+")[0];
    }

    private static String dateTime(LocalDateTime value) {
        return (value != null ? value : LocalDateTime.now()).format(DATE_TIME);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /** ELECTRICITY -> "Electricity", GARBAGE -> "Garbage". */
    private static String pretty(String enumName) {
        if (!hasText(enumName)) {
            return "Utility";
        }
        StringBuilder out = new StringBuilder();
        for (String word : enumName.toLowerCase().split("_")) {
            if (!word.isEmpty()) {
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return out.toString().trim();
    }

    private static String billingPeriod(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return DATE.format(start) + " – " + DATE.format(end);
        }
        if (end != null) {
            return "Up to " + DATE.format(end);
        }
        if (start != null) {
            return "From " + DATE.format(start);
        }
        return null;
    }

    private static String periodBadge(PaymentStatus status) {
        return switch (status) {
            case PAID -> "Paid";
            case PARTIALLY_PAID -> "Partial";
            case OVERPAID -> "Credit";
            case OVERDUE -> "Overdue";
            default -> status.name();
        };
    }

    private static EmailTone badgeTone(PaymentStatus status) {
        return switch (status) {
            case PAID -> EmailTone.SUCCESS;
            case PARTIALLY_PAID -> EmailTone.WARNING;
            case OVERDUE, FAILED -> EmailTone.DANGER;
            default -> EmailTone.INFO;
        };
    }
}