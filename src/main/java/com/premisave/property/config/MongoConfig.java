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
    @Bean
    public ApplicationRunner walletTransferIndexes(MongoTemplate mongoTemplate) {
        return args -> mongoTemplate.indexOps(WalletTransfer.class)
                .ensureIndex(new Index().on("reference", Sort.Direction.ASC).unique());
    }
}