package com.premisave.property.config;

import com.premisave.property.enums.UtilityType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Binds KES-per-unit utility rates from application.yml, e.g.:
 *
 * utility:
 *   rates:
 *     electricity: 20
 *     water: 200
 *
 * Only utility types with a configured rate can have bills auto-generated
 * from a meter reading; others require an explicit ratePerUnit override.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "utility")
public class UtilityRatesProperties {

    private Map<UtilityType, BigDecimal> rates = new HashMap<>();

    public BigDecimal getRateFor(UtilityType utilityType) {
        return rates.get(utilityType);
    }
}