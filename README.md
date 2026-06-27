# Premisave Property Management Service

## Overview

The **Premisave Property Management Service** is a production-ready Spring Boot microservice responsible for managing the entire lifecycle of residential and commercial rental properties within the **Premisave Ecosystem**.

It serves as the core domain service for:

* Property Owners (Landlords)
* Rental Properties
* Rental Units
* Tenants
* Lease Agreements
* Occupancy
* Rent Collection
* Security Deposits
* Utility Billing
* Maintenance Requests
* Property Inspections
* Notices
* Property Auditing

The service follows **Domain-Driven Design (DDD)**, **Clean Architecture**, and an **Event-Driven Microservices Architecture** to provide a scalable, secure, and maintainable foundation for enterprise property management.

---

# Technology Stack

| Technology                           | Version            |
| ------------------------------------ | ------------------ |
| Java                                 | 21                 |
| Spring Boot                          | 4.x                |
| Spring Security                      | Latest             |
| Spring Data MongoDB                  | Latest             |
| MongoDB                              | Latest             |
| Redis                                | Latest             |
| RabbitMQ                             | Latest             |
| OpenFeign                            | Latest             |
| Docker                               | Latest             |
| Docker Compose                       | Latest             |
| MapStruct                            | Latest             |
| Lombok                               | Latest             |
| Bean Validation                      | Jakarta Validation |
| Spring Boot Actuator                 | Latest             |
| Springdoc OpenAPI                    | Latest             |
| Flyway *(Optional Metadata Storage)* | Latest             |
| JUnit 5                              | Latest             |
| Mockito                              | Latest             |

---

# Architecture

The service is built following enterprise software engineering best practices.

## Architectural Principles

* Domain Driven Design (DDD)
* Clean Architecture
* SOLID Principles
* Layered Architecture
* Event-Driven Communication
* CQRS Friendly Design
* Dependency Injection
* Stateless REST APIs

---

# Project Structure

```
premisave-property-management-service/

├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── README.md

└── src
    └── main
        ├── java
        └── resources
```

The project is divided into the following layers:

### Domain Layer

Contains business entities, value objects, domain events, repositories and enums.

```
domain/
```

---

### Application Layer

Contains:

* DTOs
* Business Services
* Mappers
* Validators

```
application/
```

---

### API Layer

Contains REST Controllers.

```
api/rest/
```

---

### Infrastructure Layer

Contains external integrations including:

* RabbitMQ
* Redis
* OpenFeign
* Schedulers
* Event Consumers
* Event Publishers

```
infrastructure/
```

---

### Common Layer

Contains:

* Exception Handling
* Utilities
* Constants
* Response Models
* Auditing

```
common/
```

---

# Core Domain Modules

The service manages the following business domains.

## Property Management

* Register properties
* Update property details
* Archive properties
* Soft delete properties
* Property ownership
* Property snapshots

---

## Rental Unit Management

* Add rental units
* Unit availability
* Occupancy status
* Unit pricing
* Unit inspections

---

## Tenant Management

* Register tenants
* Tenant profile
* Emergency contacts
* Tenant blacklist
* Occupancy history

---

## Lease Management

* Lease creation
* Lease renewal
* Lease termination
* Lease expiration reminders
* Digital agreements

---

## Rent Management

* Rent schedules
* Rent payments
* Payment history
* Outstanding balances
* Late payment tracking

---

## Security Deposits

* Deposit collection
* Deposit refunds
* Deposit deductions

---

## Utility Billing

* Electricity
* Water
* Gas
* Internet
* Meter readings

---

## Maintenance Management

* Maintenance requests
* Work orders
* Maintenance scheduling
* Inspection reports

---

## Notices

* Rent reminders
* Lease notices
* Eviction notices
* Maintenance notices

---

## Reporting

Generate reports for:

* Occupancy
* Revenue
* Rent Collection
* Property Performance
* Maintenance Costs
* Financial Summary

---

# Event Driven Architecture

RabbitMQ is used for asynchronous communication.

## Published Events

* PropertyCreatedEvent
* UnitAddedEvent
* TenantRegisteredEvent
* LeaseCreatedEvent
* LeaseUpdatedEvent
* LeaseTerminatedEvent
* RentPaidEvent
* MaintenanceRequestedEvent

---

## Consumed Events

### Auth Service

* UserCreated
* UserUpdated
* UserDeleted

### Wallet Service

* PaymentCompleted
* RefundProcessed

### Booking Service

* ReservationConfirmed
* ReservationCancelled

---

# Integration with Other Microservices

The service communicates with other Premisave services using OpenFeign.

| Service           | Purpose                        |
| ----------------- | ------------------------------ |
| Auth Service      | Authentication & Authorization |
| Listing Service   | Public property listings       |
| Booking Service   | Reservations                   |
| Wallet Service    | Rent collection                |
| Messenger Service | Notifications                  |
| Media Service     | Images & Documents             |

---

# Authentication

Authentication is delegated to the Auth Service.

The Property Service validates JWT tokens using Spring Security.

Supported authentication features include:

* JWT Validation
* Bearer Authentication
* Method Security
* RBAC
* Stateless Authentication

---

# Authorization

The service uses existing platform roles.

| Role       | Description                  |
| ---------- | ---------------------------- |
| ADMIN      | Full platform administration |
| HOME_OWNER | Property owner operations    |
| CLIENT     | Tenant operations            |
| OPERATIONS | Property inspections         |
| FINANCE    | Rent reconciliation          |
| SUPPORT    | Customer support             |

No new roles are created.

---

# Security Features

* JWT Authentication
* Role Based Access Control
* Method Security
* Rate Limiting
* Soft Delete
* Optimistic Locking
* Global Exception Handling
* Validation
* Audit Logging
* Secure API Documentation

---

# Auditing

Every document stores:

* createdAt
* updatedAt
* createdBy
* updatedBy
* version
* deleted
* deletedAt

MongoDB auditing is enabled.

---

# Redis

Redis is used for:

* Frequently accessed properties
* Dashboard statistics
* Active lease cache
* Tenant cache
* Property lookup cache

---

# MongoDB

MongoDB stores all business documents including:

* Properties
* Units
* Owners
* Tenants
* Leases
* Payments
* Deposits
* Utility Bills
* Notices
* Maintenance
* Audit Logs
* Activity Feeds

---

# API Documentation

Swagger/OpenAPI is enabled.

Available after startup:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON

```
http://localhost:8080/v3/api-docs
```

---

# Monitoring

Spring Boot Actuator exposes:

* Health
* Metrics
* Environment
* Info
* Prometheus Metrics
* Thread Dumps

---

# Validation

Bean Validation is used throughout the application.

Examples include:

* Required fields
* Email validation
* Phone validation
* Lease date validation
* Payment amount validation
* Property ownership validation

---

# Logging

The application provides:

* Structured logging
* Request logging
* Audit logging
* Exception logging
* Event logging

---

# Docker

Run the entire stack using Docker Compose.

```bash
docker compose up -d
```

Included services:

* Property Service
* MongoDB
* Redis
* RabbitMQ

---

# Running the Application

## Clone

```bash
git clone https://github.com/your-org/premisave-property-management-service.git
```

---

## Configure

Copy:

```bash
.env.example
```

to

```bash
.env
```

Update the environment variables.

---

## Build

```bash
mvn clean install
```

---

## Run

```bash
mvn spring-boot:run
```

---

# Testing

Run all tests

```bash
mvn test
```

Generate coverage

```bash
mvn verify
```

Tests include:

* Unit Tests
* Integration Tests
* Repository Tests
* Controller Tests
* Service Tests
* Security Tests

---

# Future Enhancements

* AI-powered rent prediction
* Smart maintenance scheduling
* IoT meter integration
* GIS property mapping
* OCR lease processing
* Fraud detection
* Predictive maintenance
* Analytics dashboard
* Multi-tenancy support
* Multi-region deployment

---

# Production Features Checklist

* Java 21
* Spring Boot 4.x
* MongoDB
* Redis
* RabbitMQ
* JWT Authentication
* RBAC
* OpenFeign
* Docker
* Swagger/OpenAPI
* Bean Validation
* Actuator
* Auditing
* Soft Delete
* Optimistic Locking
* Pagination
* Filtering
* Global Exception Handling
* Logging
* Event-Driven Architecture
* DDD
* Clean Architecture
* SOLID Principles
* MapStruct
* Lombok
* Unit Testing
* Integration Testing
* Production Ready

---

# License

This project is proprietary software developed for the **Premisave Ecosystem**.

All rights reserved.

---

# Contributors

**Premisave Engineering Team**

Building scalable, secure, and enterprise-grade property management solutions for the modern real estate ecosystem.
