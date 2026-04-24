package com.rabbitmqoutbox.shippingservice.messaging.strategy;

import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;

public interface OutboxEventPublishStrategy {

    String eventType();

    void publish(OutboxEventEntity event);
}