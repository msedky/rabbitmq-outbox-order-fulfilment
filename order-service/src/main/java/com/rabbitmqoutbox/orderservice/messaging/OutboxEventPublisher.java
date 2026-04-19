package com.rabbitmqoutbox.orderservice.messaging;

import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;

public interface OutboxEventPublisher {

    void publish(OutboxEventEntity event);
}