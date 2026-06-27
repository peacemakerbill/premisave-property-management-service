package com.premisave.property.entity;

import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "rent_schedules")
public class RentSchedule {

    @Id
    private String id;

    private String leaseId;

    private LocalDate dueDate;
    private BigDecimal amountDue;

    private String status; // PENDING, PAID, OVERDUE

    @CreatedDate
    private LocalDateTime createdAt;
}