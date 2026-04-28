package com.rabbitmqoutbox.shippingservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqoutbox.shippingservice.exception.ShipmentNotFoundException;
import com.rabbitmqoutbox.shippingservice.messaging.event.StockReservedEvent;
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
@DisplayName("StockEventConsumer Unit Tests")
class StockEventConsumerTest {

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private Channel channel;

    private StockEventConsumer consumer;

    private UUID orderId;
    private long deliveryTag;

    @BeforeEach
    void setUp() {
        consumer = new StockEventConsumer(shipmentService);
        ReflectionTestUtils.setField(consumer, "maxRetries", 3);

        orderId = UUID.randomUUID();
        deliveryTag = 1L;
    }

    @Nested
    @DisplayName("Handle Stock Reserved Tests")
    class HandleStockReservedTests {

        @Test
        @DisplayName("Should schedule shipment and ack message successfully")
        void testHandleStockReservedSuccess() throws IOException {
            StockReservedEvent event = buildStockReservedEvent();

            consumer.handleStockReserved(event, channel, deliveryTag, null);

            verify(shipmentService, times(1)).scheduleShipment(event);
            verify(channel, times(1)).basicAck(deliveryTag, false);
            verify(channel, never()).basicReject(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject with requeue for retryable exception before max retries")
        void testHandleStockReservedRetryableFailureBeforeMaxRetries() throws IOException {
            StockReservedEvent event = buildStockReservedEvent();
            doThrow(new RuntimeException("Temporary failure"))
                    .when(shipmentService).scheduleShipment(event);

            consumer.handleStockReserved(event, channel, deliveryTag, xDeath(2L));

            verify(channel, times(1)).basicReject(deliveryTag, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when max retries reached")
        void testHandleStockReservedMaxRetriesReached() throws IOException {
            StockReservedEvent event = buildStockReservedEvent();
            doThrow(new RuntimeException("Temporary failure"))
                    .when(shipmentService).scheduleShipment(event);

            consumer.handleStockReserved(event, channel, deliveryTag, xDeath(3L));

            verify(channel, times(1)).basicReject(deliveryTag, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }

    private StockReservedEvent buildStockReservedEvent() {
        return StockReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId("CST-001")
                .customerEmail("customer@email.com")
                .deliveryAddress("123 Main St")
                .items(List.of())
                .occurredAt(Instant.now())
                .build();
    }

    private List<Map<String, Object>> xDeath(long count) {
        return List.of(Map.of("count", count));
    }
}
