package com.premisave.property.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("./")           // Root of your project (where .env is located)
                .ignoreIfMissing()         // Don't fail if .env is missing
                .load();
    }
}