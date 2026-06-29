package com.premisave.property.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InvoiceScheduler {

    /**
     * Generate monthly invoices
     */
    @Scheduled(cron = "0 0 2 1 * *") // 1st day of every month at 2 AM
    public void generateMonthlyInvoices() {
        log.info("Generating monthly invoices for all active leases...");
        // Logic to generate and send invoices
    }

    /**
     * Process overdue payments
     */
    @Scheduled(cron = "0 0 10 * * *") // Daily at 10 AM
    public void processOverduePayments() {
        log.info("Processing overdue rent payments...");
    }
}