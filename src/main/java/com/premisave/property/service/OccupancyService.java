package com.premisave.property.service;

import com.premisave.property.entity.OccupancyHistory;
import com.premisave.property.repository.OccupancyHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OccupancyService {

    private final OccupancyHistoryRepository occupancyHistoryRepository;

    // Methods for move-in, move-out, history
}