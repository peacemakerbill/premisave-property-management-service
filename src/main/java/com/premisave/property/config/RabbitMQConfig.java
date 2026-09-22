package com.premisave.property.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Jackson2JsonMessageConverter was deprecated (for removal) in Spring AMQP 4.0, in favor
    // of this Jackson 3-based converter of the same shape (no-arg constructor, same behavior).
    @Bean
    JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // Declare queues to prevent declaration errors
    @Bean
    Queue propertyAuthQueue() {
        return new Queue("property.auth.queue", true);
    }

    @Bean
    Queue propertyWalletQueue() {
        return new Queue("property.wallet.queue", true);
    }

    @Bean
    Queue propertyBookingQueue() {
        return new Queue("property.booking.queue", true);
    }
}