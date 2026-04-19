package com.rabbitmqoutbox.shippingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.shippingservice.model.enums.OutboxStatus;
import com.rabbitmqoutbox.shippingservice.repository.OutboxEventRepository;
import com.rabbitmqoutbox.shippingservice.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventServiceImpl implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveEvent(String aggregateId, String aggregateType,
                          String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            OutboxEventEntity event = OutboxEventEntity.builder()
                    .aggregateId(UUID.fromString(aggregateId))
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .payload(json)
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(event);

            log.info("Outbox event saved: aggregateId={}, eventType={}",
                    aggregateId, eventType);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEventEntity> getPendingEvents() {
        return outboxEventRepository.findByStatus(OutboxStatus.PENDING);
    }

    @Override
    @Transactional
    public void markAsPublished(OutboxEventEntity event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    @Override
    @Transactional
    public void markAsFailed(OutboxEventEntity event) {
        event.setStatus(OutboxStatus.FAILED);
        outboxEventRepository.save(event);
    }
}