package com.premisave.property.service;

import com.premisave.property.entity.RentSchedule;
import com.premisave.property.repository.RentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentScheduleService {

    private final RentScheduleRepository rentScheduleRepository;

    @Transactional
    public void generateMonthlySchedule(String leaseId, LocalDate startDate) {
        // Generate rent due dates for the lease period
    }

    public List<RentSchedule> getUpcomingPayments(String leaseId) {
        return rentScheduleRepository.findByLeaseId(leaseId);
    }

    // Run daily to check for due payments
    @Scheduled(cron = "0 0 8 * * *") // Every day at 8 AM
    public void generateReminders() {
        // Logic to find due payments and send notifications
        System.out.println("Rent reminders processed");
    }
}