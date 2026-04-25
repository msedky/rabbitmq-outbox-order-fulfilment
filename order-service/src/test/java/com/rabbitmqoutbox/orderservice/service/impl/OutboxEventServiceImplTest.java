package com.rabbitmqoutbox.orderservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.orderservice.model.enums.OutboxStatus;
import com.rabbitmqoutbox.orderservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventServiceImpl Unit Tests")
class OutboxEventServiceImplTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxEventServiceImpl outboxEventService;

    private UUID aggregateId;

    @BeforeEach
    void setUp() {
        aggregateId = UUID.randomUUID();
        outboxEventService = new OutboxEventServiceImpl(
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should save outbox event with pending status")
    void testSaveEventSuccess() throws JsonProcessingException {
        TestPayload payload = new TestPayload("ORDER-001", "PENDING");
        String jsonPayload = """
                {"orderId":"ORDER-001","status":"PENDING"}
                """;

        when(objectMapper.writeValueAsString(payload)).thenReturn(jsonPayload);

        outboxEventService.saveEvent(
                aggregateId.toString(),
                "ORDER",
                "ORDER_PLACED",
                payload
        );

        ArgumentCaptor<OutboxEventEntity> eventCaptor =
                ArgumentCaptor.forClass(OutboxEventEntity.class);

        verify(outboxEventRepository, times(1)).save(eventCaptor.capture());

        OutboxEventEntity capturedEvent = eventCaptor.getValue();

        assertThat(capturedEvent.getAggregateId()).isEqualTo(aggregateId);
        assertThat(capturedEvent.getAggregateType()).isEqualTo("ORDER");
        assertThat(capturedEvent.getEventType()).isEqualTo("ORDER_PLACED");
        assertThat(capturedEvent.getPayload()).isEqualTo(jsonPayload);
        assertThat(capturedEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when payload serialization fails")
    void testSaveEventSerializationFailure() throws JsonProcessingException {
        TestPayload payload = new TestPayload("ORDER-001", "PENDING");

        when(objectMapper.writeValueAsString(payload))
                .thenThrow(new JsonProcessingException("Serialization failed") {
                });

        assertThatThrownBy(() -> outboxEventService.saveEvent(
                aggregateId.toString(),
                "ORDER",
                "ORDER_PLACED",
                payload
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize outbox event payload");

        verify(outboxEventRepository, never()).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("Should return pending events")
    void testGetPendingEvents() {
        OutboxEventEntity event1 = buildOutboxEvent(OutboxStatus.PENDING);
        OutboxEventEntity event2 = buildOutboxEvent(OutboxStatus.PENDING);

        when(outboxEventRepository.findByStatus(OutboxStatus.PENDING))
                .thenReturn(List.of(event1, event2));

        List<OutboxEventEntity> result = outboxEventService.getPendingEvents();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(event1, event2);

        verify(outboxEventRepository, times(1))
                .findByStatus(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Should mark event as published")
    void testMarkAsPublished() {
        OutboxEventEntity event = buildOutboxEvent(OutboxStatus.PENDING);

        outboxEventService.markAsPublished(event);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getPublishedAt()).isBeforeOrEqualTo(Instant.now());

        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("Should mark event as failed")
    void testMarkAsFailed() {
        OutboxEventEntity event = buildOutboxEvent(OutboxStatus.PENDING);

        outboxEventService.markAsFailed(event);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);

        verify(outboxEventRepository, times(1)).save(event);
    }

    private OutboxEventEntity buildOutboxEvent(OutboxStatus status) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .aggregateType("ORDER")
                .eventType("ORDER_PLACED")
                .payload("{\"orderId\":\"ORDER-001\"}")
                .status(status)
                .build();
    }

    private record TestPayload(String orderId, String status) {
    }
}