package com.premisave.property.entity;

import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.LeaseType;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "leases")
public class Lease {

    @Id
    private String id;

    private String tenantId;
    private String rentalUnitId;   // null for WHOLE_PROPERTY leases
    private String propertyId;

    private LeaseType leaseType = LeaseType.UNIT;

    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;

    private LeaseStatus status = LeaseStatus.DRAFT;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}