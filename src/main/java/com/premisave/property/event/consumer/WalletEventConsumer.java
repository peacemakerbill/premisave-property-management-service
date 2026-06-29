package com.premisave.property.event.consumer;

import com.premisave.property.event.RentPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventConsumer {

    /**
     * Listen for rent payment events from Wallet Service
     */
    @RabbitListener(queues = "property.wallet.queue")
    public void handleRentPaid(RentPaidEvent event) {
        log.info("Received RentPaidEvent from Wallet Service: {}", event);

        try {
            // Update rent payment status in property service
            // Update lease balance, etc.
            log.info("Successfully processed rent payment for lease: {}", event.getLeaseId());
        } catch (Exception e) {
            log.error("Failed to process RentPaidEvent", e);
            // Handle retry or dead letter queue logic
        }
    }
}