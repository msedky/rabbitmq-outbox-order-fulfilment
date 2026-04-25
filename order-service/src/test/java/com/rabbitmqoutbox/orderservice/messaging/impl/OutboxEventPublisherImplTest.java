package com.rabbitmqoutbox.orderservice.messaging.impl;

import com.rabbitmqoutbox.orderservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.orderservice.model.enums.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPublisherImpl Unit Tests")
class OutboxEventPublisherImplTest {

    @Mock
    private OutboxEventPublishStrategy orderPlacedStrategy;

    @Mock
    private OutboxEventPublishStrategy orderCancelledStrategy;

    private OutboxEventPublisherImpl outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventPublisher = new OutboxEventPublisherImpl(
                Map.of(
                        "ORDER_PLACED", orderPlacedStrategy,
                        "ORDER_CANCELLED", orderCancelledStrategy
                )
        );
    }

    @Test
    @DisplayName("Should publish event using matching strategy")
    void testPublishWithMatchingStrategy() {
        OutboxEventEntity event = buildOutboxEvent("ORDER_PLACED");

        outboxEventPublisher.publish(event);

        verify(orderPlacedStrategy, times(1)).publish(event);
        verifyNoInteractions(orderCancelledStrategy);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no strategy exists for event type")
    void testPublishWhenStrategyNotFound() {
        OutboxEventEntity event = buildOutboxEvent("UNKNOWN_EVENT");

        assertThatThrownBy(() -> outboxEventPublisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No publish strategy found for eventType: UNKNOWN_EVENT");

        verifyNoInteractions(orderPlacedStrategy, orderCancelledStrategy);
    }

    private OutboxEventEntity buildOutboxEvent(String eventType) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("ORDER")
                .eventType(eventType)
                .payload("{\"orderId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxStatus.PENDING)
                .build();
    }
}