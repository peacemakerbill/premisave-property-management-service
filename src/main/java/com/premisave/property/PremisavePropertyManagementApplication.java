package com.premisave.property;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

// Authentication here is JWT only (JwtAuthFilter). Nothing logs in with a username and password, so Spring
// Boot's default in-memory user and its "Using generated security password" log line are switched off.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableFeignClients
@EnableScheduling
public class PremisavePropertyManagementApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        System.out.println("Dotenv loaded with " + dotenv.entries().size() + " variables");

        SpringApplication.run(PremisavePropertyManagementApplication.class, args);
    }
}