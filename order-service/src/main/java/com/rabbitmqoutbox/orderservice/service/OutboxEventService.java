package com.rabbitmqoutbox.orderservice.service;

import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;

import java.util.List;

public interface OutboxEventService {

    void saveEvent(String aggregateId, String aggregateType, String eventType, Object payload);

    List<OutboxEventEntity> getPendingEvents();

    void markAsPublished(OutboxEventEntity event);

    void markAsFailed(OutboxEventEntity event);
}