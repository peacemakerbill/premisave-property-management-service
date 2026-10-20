package com.premisave.property.config;

import com.premisave.property.entity.WalletTransfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Slf4j
@Configuration
@EnableMongoAuditing(auditorAwareRef = "auditorProvider")
public class MongoConfig {

    /**
     * Fills @CreatedBy / @LastModifiedBy with the auth-service userId of the signed-in caller
     * (JwtAuthFilter puts it on the request). Work done outside a request, such as the
     * scheduled jobs, is recorded as "system".
     */
    @Bean
    AuditorAware<String> auditorProvider() {
        return () -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            Object userId = attributes == null ? null : attributes.getAttribute("userId", RequestAttributes.SCOPE_REQUEST);
            return Optional.of(userId instanceof String id && !id.isBlank() ? id : "system");
        };
    }

    // Spring Data MongoDB 4+ does not create @Indexed indexes by default, so every index this
    // service relies on is created here at startup. createIndex is a no-op when the index exists.
    @Bean
    ApplicationRunner mongoIndexes(MongoTemplate mongo) {
        return args -> {
            // The unique reference is what makes a duplicate wallet payment impossible under
            // concurrency, so if this one cannot be created the service must not start.
            mongo.indexOps(WalletTransfer.class)
                    .createIndex(new Index().on("reference", Sort.Direction.ASC).unique());

            // Business rules: one row per key. Sparse, so older rows without the field do not clash.
            ensure(mongo, "owners", unique("userId"));
            ensure(mongo, "tenants", unique("userId"));
            ensure(mongo, "properties", unique("registrationNumber"));
            ensure(mongo, "blacklisted_tenants", unique("tenantId"));
            ensure(mongo, "security_deposits", unique("leaseId"));
            ensure(mongo, "utility_bills", unique("sourceMeterReadingId"));
            ensure(mongo, "lease_rent_unit_payments", unique("paymentReference"));
            ensure(mongo, "unit_rent_payments", unique("paymentReference"));
            ensure(mongo, "rent_balances", unique("rentalUnitId", "tenantId"));

            // Lookups the endpoints make on every request.
            ensure(mongo, "properties", plain("ownerId"));
            ensure(mongo, "rental_units", plain("propertyId"));
            ensure(mongo, "rental_units", plain("status"));
            ensure(mongo, "leases", plain("tenantId"));
            ensure(mongo, "leases", plain("rentalUnitId"));
            ensure(mongo, "leases", plain("propertyId", "status"));
            ensure(mongo, "tenants", plain("idNumber"));
            ensure(mongo, "occupancy_history", plain("rentalUnitId", "moveOutDate"));
            ensure(mongo, "occupancy_history", plain("tenantId"));
            ensure(mongo, "occupancy_history", plain("propertyId"));
            ensure(mongo, "occupancy_history", plain("leaseId"));
            ensure(mongo, "rent_schedules", plain("leaseId", "status", "dueDate"));
            ensure(mongo, "lease_rent_unit_payments", plain("leaseId"));
            ensure(mongo, "unit_rent_payments", plain("rentalUnitId"));
            ensure(mongo, "security_deposits", plain("rentalUnitId", "tenantId"));
            ensure(mongo, "meter_readings", plain("rentalUnitId", "meterType", "readingDate"));
            ensure(mongo, "utility_bills", plain("tenantId", "status"));
            ensure(mongo, "utility_bills", plain("rentalUnitId"));
            ensure(mongo, "maintenance_requests", plain("tenantId"));
            ensure(mongo, "maintenance_requests", plain("rentalUnitId", "status"));
            ensure(mongo, "work_orders", plain("maintenanceRequestId"));
            ensure(mongo, "work_orders", plain("assignedTo"));
            ensure(mongo, "notices", plain("tenantId", "noticeType", "sentAt"));
            ensure(mongo, "notices", plain("leaseId"));
            ensure(mongo, "inspections", plain("rentalUnitId"));
            ensure(mongo, "inspections", plain("createdByUserId"));
            ensure(mongo, "inspections", plain("inspectorUserId"));
            ensure(mongo, "scheduled_notices", plain("createdByOwnerId"));
        };
    }

    // An index that cannot be built (for example because rows already break a unique rule)
    // is reported and skipped, so one bad row cannot stop the whole service from starting.
    private void ensure(MongoTemplate mongo, String collection, Index index) {
        try {
            mongo.indexOps(collection).createIndex(index);
        } catch (RuntimeException e) {
            log.warn("Could not create index on '{}': {}", collection, e.getMessage());
        }
    }

    private Index unique(String... fields) {
        return plain(fields).unique().sparse();
    }

    private Index plain(String... fields) {
        Index index = new Index();
        for (String field : fields) {
            index.on(field, Sort.Direction.ASC);
        }
        return index;
    }
}
