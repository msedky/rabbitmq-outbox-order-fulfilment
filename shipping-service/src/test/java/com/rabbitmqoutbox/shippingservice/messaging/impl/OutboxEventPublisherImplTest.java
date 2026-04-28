package com.rabbitmqoutbox.shippingservice.messaging.impl;

import com.rabbitmqoutbox.shippingservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.shippingservice.model.enums.OutboxStatus;
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
    private OutboxEventPublishStrategy shipmentScheduledStrategy;

    @Mock
    private OutboxEventPublishStrategy shipmentOutForDeliveryStrategy;

    @Mock
    private OutboxEventPublishStrategy shipmentDeliveredStrategy;

    @Mock
    private OutboxEventPublishStrategy shipmentFailedStrategy;

    private OutboxEventPublisherImpl outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventPublisher = new OutboxEventPublisherImpl(
                Map.of(
                        "SHIPMENT_SCHEDULED", shipmentScheduledStrategy,
                        "SHIPMENT_OUT_FOR_DELIVERY", shipmentOutForDeliveryStrategy,
                        "SHIPMENT_DELIVERED", shipmentDeliveredStrategy,
                        "SHIPMENT_FAILED", shipmentFailedStrategy
                )
        );
    }

    @Test
    @DisplayName("Should publish SHIPMENT_SCHEDULED event using matching strategy")
    void testPublishShipmentScheduled() {
        OutboxEventEntity event = buildOutboxEvent("SHIPMENT_SCHEDULED");

        outboxEventPublisher.publish(event);

        verify(shipmentScheduledStrategy, times(1)).publish(event);
        verifyNoInteractions(shipmentOutForDeliveryStrategy,
                shipmentDeliveredStrategy, shipmentFailedStrategy);
    }

    @Test
    @DisplayName("Should publish SHIPMENT_OUT_FOR_DELIVERY event using matching strategy")
    void testPublishShipmentOutForDelivery() {
        OutboxEventEntity event = buildOutboxEvent("SHIPMENT_OUT_FOR_DELIVERY");

        outboxEventPublisher.publish(event);

        verify(shipmentOutForDeliveryStrategy, times(1)).publish(event);
        verifyNoInteractions(shipmentScheduledStrategy,
                shipmentDeliveredStrategy, shipmentFailedStrategy);
    }

    @Test
    @DisplayName("Should publish SHIPMENT_DELIVERED event using matching strategy")
    void testPublishShipmentDelivered() {
        OutboxEventEntity event = buildOutboxEvent("SHIPMENT_DELIVERED");

        outboxEventPublisher.publish(event);

        verify(shipmentDeliveredStrategy, times(1)).publish(event);
        verifyNoInteractions(shipmentScheduledStrategy,
                shipmentOutForDeliveryStrategy, shipmentFailedStrategy);
    }

    @Test
    @DisplayName("Should publish SHIPMENT_FAILED event using matching strategy")
    void testPublishShipmentFailed() {
        OutboxEventEntity event = buildOutboxEvent("SHIPMENT_FAILED");

        outboxEventPublisher.publish(event);

        verify(shipmentFailedStrategy, times(1)).publish(event);
        verifyNoInteractions(shipmentScheduledStrategy,
                shipmentOutForDeliveryStrategy, shipmentDeliveredStrategy);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no strategy exists for event type")
    void testPublishWhenStrategyNotFound() {
        OutboxEventEntity event = buildOutboxEvent("UNKNOWN_EVENT");

        assertThatThrownBy(() -> outboxEventPublisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No publish strategy found for eventType: UNKNOWN_EVENT");

        verifyNoInteractions(shipmentScheduledStrategy, shipmentOutForDeliveryStrategy,
                shipmentDeliveredStrategy, shipmentFailedStrategy);
    }

    private OutboxEventEntity buildOutboxEvent(String eventType) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("SHIPMENT")
                .eventType(eventType)
                .payload("{\"orderId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxStatus.PENDING)
                .build();
    }
}