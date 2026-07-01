package com.premisave.property.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    // MongoDB specific configuration can be added here
    // Auditing is enabled via @EnableMongoAuditing
}