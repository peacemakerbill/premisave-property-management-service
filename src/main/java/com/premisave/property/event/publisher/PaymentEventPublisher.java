package com.premisave.property.event.publisher;

import com.premisave.property.event.PropertyCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPropertyCreated(PropertyCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend("property.exchange", "property.created", event);
            log.info("Published PropertyCreatedEvent for property: {}", event.getPropertyId());
        } catch (Exception e) {
            log.error("Failed to publish PropertyCreatedEvent", e);
        }
    }
}