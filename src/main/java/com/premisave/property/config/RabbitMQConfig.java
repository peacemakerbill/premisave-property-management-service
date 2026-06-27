package com.premisave.property.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String PROPERTY_EXCHANGE = "property.exchange";
    public static final String LEASE_EXCHANGE = "lease.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // Queues
    public static final String WALLET_PAYMENT_QUEUE = "wallet.payment.queue";
    public static final String BOOKING_SYNC_QUEUE = "booking.sync.queue";
    public static final String AUTH_USER_QUEUE = "auth.user.queue";

    @Bean
    public DirectExchange propertyExchange() {
        return new DirectExchange(PROPERTY_EXCHANGE);
    }

    @Bean
    public DirectExchange leaseExchange() {
        return new DirectExchange(LEASE_EXCHANGE);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue walletPaymentQueue() {
        return new Queue(WALLET_PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue bookingSyncQueue() {
        return new Queue(BOOKING_SYNC_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}