package com.rabbitmqoutbox.warehouseservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqoutbox.warehouseservice.exception.InsufficientStockException;
import com.rabbitmqoutbox.warehouseservice.exception.StockNotFoundException;
import com.rabbitmqoutbox.warehouseservice.messaging.event.OrderCancelledEvent;
import com.rabbitmqoutbox.warehouseservice.messaging.event.OrderPlacedEvent;
import com.rabbitmqoutbox.warehouseservice.messaging.event.ShipmentOutForDeliveryEvent;
import com.rabbitmqoutbox.warehouseservice.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer Unit Tests")
class OrderEventConsumerTest {

    @Mock
    private StockService stockService;

    @Mock
    private Channel channel;

    private OrderEventConsumer consumer;

    private UUID orderId;
    private long deliveryTag;

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(stockService);
        ReflectionTestUtils.setField(consumer, "maxRetries", 3);

        orderId = UUID.randomUUID();
        deliveryTag = 1L;
    }

    @Nested
    @DisplayName("Handle Order Placed Tests")
    class HandleOrderPlacedTests {

        @Test
        @DisplayName("Should reserve stock and ack message successfully")
        void testHandleOrderPlacedSuccess() throws IOException {
            OrderPlacedEvent event = buildOrderPlacedEvent();

            consumer.handleOrderPlaced(event, channel, deliveryTag, null);

            verify(stockService, times(1)).reserveStock(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when stock is insufficient")
        void testHandleOrderPlacedInsufficientStock() throws IOException {
            OrderPlacedEvent event = buildOrderPlacedEvent();

            doThrow(new InsufficientStockException("Insufficient stock"))
                    .when(stockService)
                    .reserveStock(event);

            consumer.handleOrderPlaced(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when stock not found")
        void testHandleOrderPlacedStockNotFound() throws IOException {
            OrderPlacedEvent event = buildOrderPlacedEvent();

            doThrow(new StockNotFoundException("Stock not found"))
                    .when(stockService)
                    .reserveStock(event);

            consumer.handleOrderPlaced(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue for retryable exception before max retries")
        void testHandleOrderPlacedRetryableFailureBeforeMaxRetries() throws IOException {
            OrderPlacedEvent event = buildOrderPlacedEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(stockService)
                    .reserveStock(event);

            consumer.handleOrderPlaced(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleOrderPlacedMaxRetriesReached() throws IOException {
            OrderPlacedEvent event = buildOrderPlacedEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(stockService)
                    .reserveStock(event);

            consumer.handleOrderPlaced(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("Handle Order Cancelled Tests")
    class HandleOrderCancelledTests {

        @Test
        @DisplayName("Should release stock and ack message successfully")
        void testHandleOrderCancelledSuccess() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();

            consumer.handleOrderCancelled(event, channel, deliveryTag, null);

            verify(stockService, times(1)).releaseStock(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when stock not found")
        void testHandleOrderCancelledStockNotFound() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();

            doThrow(new StockNotFoundException("Stock not found"))
                    .when(stockService)
                    .releaseStock(event);

            consumer.handleOrderCancelled(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue for retryable exception before max retries")
        void testHandleOrderCancelledRetryableFailureBeforeMaxRetries() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(stockService)
                    .releaseStock(event);

            consumer.handleOrderCancelled(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleOrderCancelledMaxRetriesReached() throws IOException {
            OrderCancelledEvent event = buildOrderCancelledEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(stockService)
                    .releaseStock(event);

            consumer.handleOrderCancelled(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("Handle Shipment Out For Delivery Tests")
    class HandleShipmentOutForDeliveryTests {

        @Test
        @DisplayName("Should fulfill stock and ack message successfully")
        void testHandleShipmentOutForDeliverySuccess() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, null);

            verify(stockService, times(1)).fulfillStock(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when stock not found")
        void testHandleShipmentOutForDeliveryStockNotFound() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            doThrow(new StockNotFoundException("Stock not found"))
                    .when(stockService)
                    .fulfillStock(event);

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, null);

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue for retryable exception before max retries")
        void testHandleShipmentOutForDeliveryRetryableFailureBeforeMaxRetries() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(stockService)
                    .fulfillStock(event);

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleShipmentOutForDeliveryMaxRetriesReached() throws IOException {
            ShipmentOutForDeliveryEvent event = buildShipmentOutForDeliveryEvent();

            doThrow(new RuntimeException("Temporary failure"))
                    .when(stockService)
                    .fulfillStock(event);

            consumer.handleShipmentOutForDelivery(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    private OrderPlacedEvent buildOrderPlacedEvent() {
        return OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .deliveryAddress("123 Main St")
                .items(List.of())
                .build();
    }

    private OrderCancelledEvent buildOrderCancelledEvent() {
        return OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .build();
    }

    private ShipmentOutForDeliveryEvent buildShipmentOutForDeliveryEvent() {
        return ShipmentOutForDeliveryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .shipmentId(UUID.randomUUID())
                .build();
    }

    private List<Map<String, Object>> xDeath(long count) {
        return List.of(Map.of("count", count));
    }
}