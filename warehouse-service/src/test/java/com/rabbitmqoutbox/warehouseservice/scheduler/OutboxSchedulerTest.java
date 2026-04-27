package com.rabbitmqoutbox.warehouseservice.scheduler;

import com.rabbitmqoutbox.warehouseservice.messaging.OutboxEventPublisher;
import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.warehouseservice.model.enums.OutboxStatus;
import com.rabbitmqoutbox.warehouseservice.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxScheduler Unit Tests")
class OutboxSchedulerTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private OutboxScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxScheduler(outboxEventService, outboxEventPublisher);
    }

    @Test
    @DisplayName("Should do nothing when there are no pending events")
    void testProcessPendingEventsWhenNoPendingEvents() {
        when(outboxEventService.getPendingEvents()).thenReturn(List.of());

        scheduler.processPendingEvents();

        verify(outboxEventService, times(1)).getPendingEvents();
        verifyNoInteractions(outboxEventPublisher);
        verify(outboxEventService, never()).markAsPublished(any());
        verify(outboxEventService, never()).markAsFailed(any());
    }

    @Test
    @DisplayName("Should publish pending event and mark it as published")
    void testProcessPendingEventsSuccessfully() {
        OutboxEventEntity event = buildOutboxEvent("STOCK_RESERVED");

        when(outboxEventService.getPendingEvents()).thenReturn(List.of(event));

        scheduler.processPendingEvents();

        verify(outboxEventService, times(1)).getPendingEvents();
        verify(outboxEventPublisher, times(1)).publish(event);
        verify(outboxEventService, times(1)).markAsPublished(event);
        verify(outboxEventService, never()).markAsFailed(any());
    }

    @Test
    @DisplayName("Should mark event as failed when publishing fails")
    void testProcessPendingEventsWhenPublishingFails() {
        OutboxEventEntity event = buildOutboxEvent("STOCK_RESERVED");

        when(outboxEventService.getPendingEvents()).thenReturn(List.of(event));

        doThrow(new RuntimeException("RabbitMQ is down"))
                .when(outboxEventPublisher)
                .publish(event);

        scheduler.processPendingEvents();

        verify(outboxEventService, times(1)).getPendingEvents();
        verify(outboxEventPublisher, times(1)).publish(event);
        verify(outboxEventService, times(1)).markAsFailed(event);
        verify(outboxEventService, never()).markAsPublished(any());
    }

    @Test
    @DisplayName("Should continue processing next events when one event fails")
    void testProcessPendingEventsContinuesAfterFailure() {
        OutboxEventEntity failedEvent = buildOutboxEvent("STOCK_RESERVED");
        OutboxEventEntity successfulEvent = buildOutboxEvent("STOCK_RESERVED");

        when(outboxEventService.getPendingEvents())
                .thenReturn(List.of(failedEvent, successfulEvent));

        doThrow(new RuntimeException("RabbitMQ is down"))
                .when(outboxEventPublisher)
                .publish(failedEvent);

        scheduler.processPendingEvents();

        verify(outboxEventPublisher, times(1)).publish(failedEvent);
        verify(outboxEventPublisher, times(1)).publish(successfulEvent);

        verify(outboxEventService, times(1)).markAsFailed(failedEvent);
        verify(outboxEventService, times(1)).markAsPublished(successfulEvent);
    }

    private OutboxEventEntity buildOutboxEvent(String eventType) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("STOCK")
                .eventType(eventType)
                .payload("{\"orderId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxStatus.PENDING)
                .build();
    }
}