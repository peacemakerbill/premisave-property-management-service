package com.premisave.property.scheduler;

import com.premisave.property.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaseScheduler {

    private final LeaseRepository leaseRepository;

    /**
     * Check for expiring leases every day at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkExpiringLeases() {
        log.info("Running lease expiration check...");
        // Find leases ending in next 30 days and send renewal notices
    }

    /**
     * Auto-terminate expired leases
     */
    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    public void terminateExpiredLeases() {
        log.info("Terminating expired leases...");
    }
}