package com.premisave.property.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    /**
     * Listen for booking-related events (from Booking Service)
     */
    @RabbitListener(queues = "property.booking.queue")
    public void handleBookingEvent(Object event) {
        log.info("Received booking event: {}", event);

        // Example: Block unit availability when booking is confirmed
    }
}