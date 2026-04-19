package com.rabbitmqoutbox.shippingservice.service;

import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;

import java.util.List;

public interface OutboxEventService {

    void saveEvent(String aggregateId, String aggregateType, String eventType, Object payload);

    List<OutboxEventEntity> getPendingEvents();

    void markAsPublished(OutboxEventEntity event);

    void markAsFailed(OutboxEventEntity event);
}