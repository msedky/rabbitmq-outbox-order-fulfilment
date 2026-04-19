package com.rabbitmqoutbox.warehouseservice.messaging;

import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;

public interface OutboxEventPublisher {

    void publish(OutboxEventEntity event);
}