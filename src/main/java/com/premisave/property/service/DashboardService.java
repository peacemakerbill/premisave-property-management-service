package com.premisave.property.service;

import com.premisave.property.dto.response.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    public DashboardSummaryResponse getOwnerDashboard(String ownerId) {
        // Aggregate data from multiple repositories
        return new DashboardSummaryResponse();
    }
}