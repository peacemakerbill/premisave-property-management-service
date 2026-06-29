package com.premisave.property.event.publisher;

import com.premisave.property.event.LeaseCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaseEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishLeaseCreated(LeaseCreatedEvent event) {
        rabbitTemplate.convertAndSend("lease.exchange", "lease.created", event);
        log.info("Published LeaseCreatedEvent for lease: {}", event.getLeaseId());
    }

    public void publishLeaseTerminated(String leaseId, String reason) {
        // Publish termination event
    }
}