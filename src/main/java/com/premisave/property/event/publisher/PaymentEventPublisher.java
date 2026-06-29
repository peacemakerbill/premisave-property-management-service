package com.premisave.property.event.publisher;

import com.premisave.property.event.RentPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishRentPaid(RentPaidEvent event) {
        rabbitTemplate.convertAndSend("payment.exchange", "rent.paid", event);
        log.info("Published RentPaidEvent for lease: {}", event.getLeaseId());
    }
}