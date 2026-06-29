package com.premisave.property.service;

import com.premisave.property.entity.PropertySnapshot;
import com.premisave.property.repository.PropertySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertySnapshotService {

    private final PropertySnapshotRepository snapshotRepository;

    public void createSnapshot(String propertyId, String snapshotData) {
        PropertySnapshot snapshot = new PropertySnapshot();
        snapshot.setPropertyId(propertyId);
        snapshot.setSnapshotData(snapshotData);
        snapshotRepository.save(snapshot);
    }
}