package com.rabbitmqoutbox.shippingservice.messaging;

import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;

public interface OutboxEventPublisher {

    void publish(OutboxEventEntity event);
}