package com.rabbitmqoutbox.warehouseservice.messaging.impl;

import com.rabbitmqoutbox.warehouseservice.messaging.OutboxEventPublisher;
import com.rabbitmqoutbox.warehouseservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherImpl implements OutboxEventPublisher {

    private final Map<String, OutboxEventPublishStrategy> outboxPublishStrategyMap;

    @Override
    public void publish(OutboxEventEntity event) {
        OutboxEventPublishStrategy strategy =
                outboxPublishStrategyMap.get(event.getEventType());

        if (strategy == null) {
            throw new IllegalStateException(
                    "No publish strategy found for eventType: " + event.getEventType());
        }

        strategy.publish(event);
    }
}