package com.rabbitmqoutbox.orderservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqoutbox.orderservice.exception.OrderNotFoundException;
import com.rabbitmqoutbox.orderservice.messaging.event.*;
import com.rabbitmqoutbox.orderservice.service.OrderService;
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
    private OrderService orderService;

    @Mock
    private Channel channel;

    private OrderEventConsumer consumer;

    private UUID orderId;
    private long deliveryTag;

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(orderService);
        ReflectionTestUtils.setField(consumer, "maxRetries", 3);

        orderId = UUID.randomUUID();
        deliveryTag = 1L;
    }

    @Nested
    @DisplayName("Handle Stock Reserved Tests")
    class HandleStockReservedTests {

        @Test
        @DisplayName("Should confirm order and ack message successfully")
        void testHandleStockReservedSuccess() throws IOException {
            StockReservedEvent event = buildStockReservedEvent();

            consumer.handleStockReserved(event, channel, deliveryTag, null);

            verify(orderService, times(1)).confirmOrder(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when order not found")
        void testHandleStockReservedOrderNotFound() throws IOException {
            StockReservedEvent event = buildStockReservedEvent();

            doThrow(new OrderNotFoundException("Order not found"))
                    .when(orderService)
                    .confirmOrder(event);

            consumer.handleStockReserved(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue for retryable exception before max retries")
        void testHandleStockReservedRetryableFailureBeforeMaxRetries() throws IOException {
            StockReservedEvent event = buildStockReservedEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .confirmOrder(event);

            consumer.handleStockReserved(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleStockReservedMaxRetriesReached() throws IOException {
            StockReservedEvent event = buildStockReservedEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .confirmOrder(event);

            consumer.handleStockReserved(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("Handle Shipment Scheduled Tests")
    class HandleShipmentScheduledTests {

        @Test
        @DisplayName("Should mark order as shipped and ack message successfully")
        void testHandleShipmentScheduledSuccess() throws IOException {
            ShipmentScheduledEvent event = buildShipmentScheduledEvent();

            consumer.handleShipmentScheduled(event, channel, deliveryTag, null);

            verify(orderService, times(1)).markAsShipped(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when order not found")
        void testHandleShipmentScheduledOrderNotFound() throws IOException {
            ShipmentScheduledEvent event = buildShipmentScheduledEvent();

            doThrow(new OrderNotFoundException("Order not found"))
                    .when(orderService)
                    .markAsShipped(event);

            consumer.handleShipmentScheduled(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue for retryable exception before max retries")
        void testHandleShipmentScheduledRetryableFailureBeforeMaxRetries() throws IOException {
            ShipmentScheduledEvent event = buildShipmentScheduledEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsShipped(event);

            consumer.handleShipmentScheduled(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleShipmentScheduledMaxRetriesReached() throws IOException {
            ShipmentScheduledEvent event = buildShipmentScheduledEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsShipped(event);

            consumer.handleShipmentScheduled(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("Handle Shipment Out For Delivery Tests")
    class HandleShipmentOutForDeliveryTests {

        @Test
        @DisplayName("Should mark order as out for delivery and ack message successfully")
        void testHandleShipmentOutForDeliverySuccess() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, null);

            verify(orderService, times(1)).markAsOutForDelivery(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when order not found")
        void testHandleShipmentOutForDeliveryOrderNotFound() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            doThrow(new OrderNotFoundException("Order not found"))
                    .when(orderService)
                    .markAsOutForDelivery(event);

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue before max retries")
        void testHandleShipmentOutForDeliveryRetryableFailureBeforeMaxRetries() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsOutForDelivery(event);

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleShipmentOutForDeliveryMaxRetriesReached() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsOutForDelivery(event);

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("Handle Shipment Delivered Tests")
    class HandleShipmentDeliveredTests {

        @Test
        @DisplayName("Should mark order as delivered and ack message successfully")
        void testHandleShipmentDeliveredSuccess() throws IOException {
            ShipmentDeliveredEvent event = buildShipmentDeliveredEvent();

            consumer.handleShipmentDelivered(event, channel, deliveryTag, null);

            verify(orderService, times(1)).markAsDelivered(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when order not found")
        void testHandleShipmentDeliveredOrderNotFound() throws IOException {
            ShipmentDeliveredEvent event = buildShipmentDeliveredEvent();

            doThrow(new OrderNotFoundException("Order not found"))
                    .when(orderService)
                    .markAsDelivered(event);

            consumer.handleShipmentDelivered(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue before max retries")
        void testHandleShipmentDeliveredRetryableFailureBeforeMaxRetries() throws IOException {
            ShipmentDeliveredEvent event = buildShipmentDeliveredEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsDelivered(event);

            consumer.handleShipmentDelivered(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleShipmentDeliveredMaxRetriesReached() throws IOException {
            ShipmentDeliveredEvent event = buildShipmentDeliveredEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsDelivered(event);

            consumer.handleShipmentDelivered(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("Handle Shipment Failed Tests")
    class HandleShipmentFailedTests {

        @Test
        @DisplayName("Should mark order as failed and ack message successfully")
        void testHandleShipmentFailedSuccess() throws IOException {
            ShipmentFailedEvent event = buildShipmentFailedEvent();

            consumer.handleShipmentFailed(event, channel, deliveryTag, null);

            verify(orderService, times(1)).markAsFailed(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when order not found")
        void testHandleShipmentFailedOrderNotFound() throws IOException {
            ShipmentFailedEvent event = buildShipmentFailedEvent();

            doThrow(new OrderNotFoundException("Order not found"))
                    .when(orderService)
                    .markAsFailed(event);

            consumer.handleShipmentFailed(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue before max retries")
        void testHandleShipmentFailedRetryableFailureBeforeMaxRetries() throws IOException {
            ShipmentFailedEvent event = buildShipmentFailedEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsFailed(event);

            consumer.handleShipmentFailed(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleShipmentFailedMaxRetriesReached() throws IOException {
            ShipmentFailedEvent event = buildShipmentFailedEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(orderService)
                    .markAsFailed(event);

            consumer.handleShipmentFailed(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    private StockReservedEvent buildStockReservedEvent() {
        return StockReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .deliveryAddress("123 Main St")
                .items(List.of())
                .occurredAt(Instant.now())
                .build();
    }

    private ShipmentScheduledEvent buildShipmentScheduledEvent() {
        return ShipmentScheduledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .shipmentId(UUID.randomUUID())
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .deliveryAddress("123 Main St")
                .scheduledAt(Instant.now())
                .occurredAt(Instant.now())
                .build();
    }

    private ShipmentOutForDeliveryEvent buildShipmentOutForDeliveryEvent() {
        return ShipmentOutForDeliveryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .shipmentId(UUID.randomUUID())
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .deliveryAddress("123 Main St")
                .dispatchedAt(Instant.now())
                .occurredAt(Instant.now())
                .build();
    }

    private ShipmentDeliveredEvent buildShipmentDeliveredEvent() {
        return ShipmentDeliveredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .shipmentId(UUID.randomUUID())
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .deliveredAt(Instant.now())
                .occurredAt(Instant.now())
                .build();
    }

    private ShipmentFailedEvent buildShipmentFailedEvent() {
        return ShipmentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .shipmentId(UUID.randomUUID())
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .failureReason("Delivery address not found")
                .failedAt(Instant.now())
                .occurredAt(Instant.now())
                .build();
    }

    private List<Map<String, Object>> xDeath(long count) {
        return List.of(Map.of("count", count));
    }
}