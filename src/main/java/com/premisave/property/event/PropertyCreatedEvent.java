package com.premisave.property.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyCreatedEvent {

    private String propertyId;
    private String ownerId;
    private String title;
    private String propertyType;
    private LocalDateTime createdAt;
}