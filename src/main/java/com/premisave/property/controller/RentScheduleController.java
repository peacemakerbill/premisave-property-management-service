package com.premisave.property.controller;

import com.premisave.property.dto.response.RentScheduleResponse;
import com.premisave.property.security.AccessGuard;
import com.premisave.property.service.RentScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rent-schedules")
@RequiredArgsConstructor
public class RentScheduleController {

    private final RentScheduleService rentScheduleService;
    private final AccessGuard guard;

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<List<RentScheduleResponse>> getUpcomingPayments(@PathVariable String leaseId) {
        guard.requireLeaseParticipant(leaseId);
        return ResponseEntity.ok(rentScheduleService.getUpcomingPayments(leaseId));
    }
}
