package com.rabbitmqoutbox.warehouseservice.messaging.strategy;

import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;

public interface OutboxEventPublishStrategy {

    String eventType();

    void publish(OutboxEventEntity event);
}