package com.premisave.property.scheduler;

import com.premisave.property.repository.RentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentReminderScheduler {

    private final RentScheduleRepository rentScheduleRepository;

    /**
     * Send rent reminders 3 days before due date
     */
    @Scheduled(cron = "0 0 8 * * *") // Every day at 8:00 AM
    public void sendRentReminders() {
        log.info("Sending rent due reminders...");
        // Query for upcoming due payments and trigger notifications
    }
}