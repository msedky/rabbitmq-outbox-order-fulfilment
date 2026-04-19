package com.rabbitmqoutbox.warehouseservice.scheduler;

import com.rabbitmqoutbox.warehouseservice.messaging.OutboxEventPublisher;
import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.warehouseservice.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxEventService outboxEventService;
    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventService.getPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("OutboxScheduler found {} pending event(s) to publish",
                pendingEvents.size());

        for (OutboxEventEntity event : pendingEvents) {
            try {
                outboxEventPublisher.publish(event);
                outboxEventService.markAsPublished(event);
                log.info("Outbox event published: id={}, eventType={}",
                        event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: id={}, eventType={}",
                        event.getId(), event.getEventType(), e);
                outboxEventService.markAsFailed(event);
            }
        }
    }
}