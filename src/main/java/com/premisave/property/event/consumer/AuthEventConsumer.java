package com.premisave.property.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    /**
     * Listen for user-related events from Auth Service
     * (e.g., user verified, account deactivated)
     */
    @RabbitListener(queues = "property.auth.queue")
    public void handleUserEvent(Object event) {
        log.info("Received auth event: {}", event);

        // Example: Sync user status with tenant/owner records
    }
}