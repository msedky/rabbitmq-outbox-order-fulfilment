package com.rabbitmqoutbox.shippingservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqoutbox.shippingservice.exception.ShipmentNotFoundException;
import com.rabbitmqoutbox.shippingservice.messaging.event.OrderCancelledEvent;
import com.rabbitmqoutbox.shippingservice.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer Unit Tests")
class OrderEventConsumerTest {

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private Channel channel;

    private OrderEventConsumer consumer;

    private UUID orderId;
    private long deliveryTag;

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(shipmentService);
        ReflectionTestUtils.setField(consumer, "maxRetries", 3);

        orderId = UUID.randomUUID();
        deliveryTag = 1L;
    }

    @Nested
    @DisplayName("Handle Order Cancelled Tests")
    class HandleOrderCancelledTests {

        @Test
        @DisplayName("Should cancel shipment and ack message successfully")
        void testHandleOrderCancelledSuccess() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();

            consumer.handleOrderCancelled(event, channel, deliveryTag, null);

            verify(shipmentService, times(1)).cancelShipment(orderId);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue on IllegalStateException")
        void testHandleOrderCancelledIllegalState() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();
            doThrow(new IllegalStateException("Cannot cancel a delivered shipment"))
                    .when(shipmentService).cancelShipment(orderId);

            consumer.handleOrderCancelled(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue on ShipmentNotFoundException")
        void testHandleOrderCancelledShipmentNotFound() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();
            doThrow(new ShipmentNotFoundException("Shipment not found"))
                    .when(shipmentService).cancelShipment(orderId);

            consumer.handleOrderCancelled(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue for retryable exception before max retries")
        void testHandleOrderCancelledRetryableFailureBeforeMaxRetries() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();
            doThrow(new RuntimeException("Temporary failure"))
                    .when(shipmentService).cancelShipment(orderId);

            consumer.handleOrderCancelled(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleOrderCancelledMaxRetriesReached() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();
            doThrow(new RuntimeException("Temporary failure"))
                    .when(shipmentService).cancelShipment(orderId);

            consumer.handleOrderCancelled(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    private OrderCancelledEvent buildOrderCancelledEvent() {
        return OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId("CST-001")
                .customerEmail("customer@email.com")
                .occurredAt(Instant.now())
                .build();
    }

    private List<Map<String, Object>> xDeath(long count) {
        return List.of(Map.of("count", count));
    }
}
