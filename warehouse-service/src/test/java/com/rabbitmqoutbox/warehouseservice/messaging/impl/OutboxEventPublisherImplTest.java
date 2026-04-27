package com.rabbitmqoutbox.warehouseservice.messaging.impl;

import com.rabbitmqoutbox.warehouseservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.warehouseservice.model.enums.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPublisherImpl Unit Tests")
class OutboxEventPublisherImplTest {

    @Mock
    private OutboxEventPublishStrategy stockReservedStrategy;

    private OutboxEventPublisherImpl outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventPublisher = new OutboxEventPublisherImpl(
                Map.of("STOCK_RESERVED", stockReservedStrategy)
        );
    }

    @Test
    @DisplayName("Should publish event using matching strategy")
    void testPublishWithMatchingStrategy() {
        OutboxEventEntity event = buildOutboxEvent("STOCK_RESERVED");

        outboxEventPublisher.publish(event);

        verify(stockReservedStrategy, times(1)).publish(event);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no strategy exists for event type")
    void testPublishWhenStrategyNotFound() {
        OutboxEventEntity event = buildOutboxEvent("UNKNOWN_EVENT");

        assertThatThrownBy(() -> outboxEventPublisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No publish strategy found for eventType: UNKNOWN_EVENT");

        verifyNoInteractions(stockReservedStrategy);
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