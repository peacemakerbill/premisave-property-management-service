package com.premisave.property.config;

import com.premisave.property.entity.WalletTransfer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    // Spring Data MongoDB 4+ does not auto-create @Indexed indexes by default.
    // The unique reference index is what makes duplicate wallet payments
    // impossible under concurrency, so it is created explicitly at startup.
    //
    // IndexOperations.ensureIndex(IndexDefinition) was deprecated (for removal) in Spring Data
    // MongoDB 4.5, in favor of createIndex(IndexDefinition) — same signature and return value.
    @Bean
    ApplicationRunner walletTransferIndexes(MongoTemplate mongoTemplate) {
        return args -> mongoTemplate.indexOps(WalletTransfer.class)
                .createIndex(new Index().on("reference", Sort.Direction.ASC).unique());
    }
}