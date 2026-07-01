package com.premisave.property.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Declare queues to prevent declaration errors
    @Bean
    public Queue propertyAuthQueue() {
        return new Queue("property.auth.queue", true);
    }

    @Bean
    public Queue propertyWalletQueue() {
        return new Queue("property.wallet.queue", true);
    }

    @Bean
    public Queue propertyBookingQueue() {
        return new Queue("property.booking.queue", true);
    }
}