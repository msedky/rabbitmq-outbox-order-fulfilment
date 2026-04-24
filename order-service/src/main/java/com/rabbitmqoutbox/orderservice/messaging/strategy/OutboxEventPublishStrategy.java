package com.rabbitmqoutbox.orderservice.messaging.strategy;

import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;

public interface OutboxEventPublishStrategy {

    String eventType();

    void publish(OutboxEventEntity event);
}