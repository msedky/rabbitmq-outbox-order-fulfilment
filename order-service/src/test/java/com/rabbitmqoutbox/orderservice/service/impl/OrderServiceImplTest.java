package com.rabbitmqoutbox.orderservice.service.impl;

import com.rabbitmqoutbox.orderservice.exception.OrderNotFoundException;
import com.rabbitmqoutbox.orderservice.mapper.OrderMapper;
import com.rabbitmqoutbox.orderservice.mapper.OrderMapperImpl;
import com.rabbitmqoutbox.orderservice.messaging.event.*;
import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderItemRequest;
import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderRequest;
import com.rabbitmqoutbox.orderservice.model.dto.response.OrderResponse;
import com.rabbitmqoutbox.orderservice.model.entity.OrderEntity;
import com.rabbitmqoutbox.orderservice.model.entity.OrderItemEntity;
import com.rabbitmqoutbox.orderservice.model.enums.OrderStatus;
import com.rabbitmqoutbox.orderservice.repository.OrderRepository;
import com.rabbitmqoutbox.orderservice.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventService outboxEventService;

    private OrderMapper orderMapper;
    private OrderServiceImpl orderService;

    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;
    private String currency;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = "CST-001";
        customerEmail = "customer@email.com";
        deliveryAddress = "123 Main St";
        currency = "USD";

        orderMapper = new OrderMapperImpl();
        orderService = new OrderServiceImpl(orderRepository, outboxEventService, orderMapper);
    }

    private CreateOrderRequest buildValidRequest() {
        return CreateOrderRequest.builder()
                .customerId(customerId)
                .customerEmail(customerEmail)
                .deliveryAddress(deliveryAddress)
                .currency(currency)
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId("PROD-001")
                                .productName("Product 1")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(50.0))
                                .build(),
                        CreateOrderItemRequest.builder()
                                .productId("PROD-002")
                                .productName("Product 2")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(30.0))
                                .build()
                ))
                .build();
    }

    private OrderEntity buildSavedOrder(OrderStatus status) {
        return OrderEntity.builder()
                .id(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .deliveryAddress(deliveryAddress)
                .totalAmount(BigDecimal.valueOf(130.0))
                .currency(currency)
                .status(status)
                .items(List.of(
                        OrderItemEntity.builder()
                                .id(UUID.randomUUID())
                                .productId("PROD-001")
                                .productName("Product 1")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(50.0))
                                .build(),
                        OrderItemEntity.builder()
                                .id(UUID.randomUUID())
                                .productId("PROD-002")
                                .productName("Product 2")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(30.0))
                                .build()
                ))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully with valid request")
        void testCreateOrderSuccess() {
            CreateOrderRequest request = buildValidRequest();
            OrderEntity savedOrder = buildSavedOrder(OrderStatus.PENDING);

            when(orderRepository.saveAndFlush(any(OrderEntity.class)))
                    .thenReturn(savedOrder);

            OrderResponse result = orderService.create(request);

            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getStatus()).isEqualTo("PENDING");

            ArgumentCaptor<OrderEntity> orderCaptor =
                    ArgumentCaptor.forClass(OrderEntity.class);

            verify(orderRepository, times(1)).saveAndFlush(orderCaptor.capture());

            OrderEntity capturedOrder = orderCaptor.getValue();

            assertThat(capturedOrder.getCustomerId()).isEqualTo(customerId);
            assertThat(capturedOrder.getCustomerEmail()).isEqualTo(customerEmail);
            assertThat(capturedOrder.getDeliveryAddress()).isEqualTo(deliveryAddress);
            assertThat(capturedOrder.getCurrency()).isEqualTo(currency);
            assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(capturedOrder.getTotalAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(130.0));
            assertThat(capturedOrder.getItems()).hasSize(2);
            assertThat(capturedOrder.getItems())
                    .extracting(OrderItemEntity::getProductId)
                    .containsExactlyInAnyOrder("PROD-001", "PROD-002");

            ArgumentCaptor<Object> eventCaptor =
                    ArgumentCaptor.forClass(Object.class);

            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("ORDER"),
                    eq("ORDER_PLACED"),
                    eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue()).isInstanceOf(OrderPlacedEvent.class);

            OrderPlacedEvent capturedEvent =
                    (OrderPlacedEvent) eventCaptor.getValue();

            assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(capturedEvent.getTotalAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(130.0));
            assertThat(capturedEvent.getItems()).hasSize(2);
            assertThat(capturedEvent.getItems())
                    .extracting(OrderPlacedEventItem::getProductId)
                    .containsExactlyInAnyOrder("PROD-001", "PROD-002");
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel PENDING order successfully")
        void testCancelOrderSuccess() {
            OrderEntity order = buildSavedOrder(OrderStatus.PENDING);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            OrderResponse result = orderService.cancel(orderId);

            assertThat(result).isNotNull();

            verify(orderRepository, times(1)).save(order);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelledAt()).isNotNull();

            ArgumentCaptor<Object> eventCaptor =
                    ArgumentCaptor.forClass(Object.class);

            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("ORDER"),
                    eq("ORDER_CANCELLED"),
                    eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue()).isInstanceOf(OrderCancelledEvent.class);

            OrderCancelledEvent capturedEvent =
                    (OrderCancelledEvent) eventCaptor.getValue();

            assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(capturedEvent.getCustomerId()).isEqualTo(customerId);
            assertThat(capturedEvent.getCustomerEmail()).isEqualTo(customerEmail);
            assertThat(capturedEvent.getEventId()).isNotBlank();
            assertThat(capturedEvent.getOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void testCancelOrderNotFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancel(orderId))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when order is already cancelled")
        void testCancelOrderAlreadyCancelled() {
            OrderEntity order = buildSavedOrder(OrderStatus.CANCELLED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancel(orderId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already cancelled");

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when order is delivered")
        void testCancelOrderDelivered() {
            OrderEntity order = buildSavedOrder(OrderStatus.DELIVERED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancel(orderId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("delivered");

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when order is out for delivery")
        void testCancelOrderOutForDelivery() {
            OrderEntity order = buildSavedOrder(OrderStatus.OUT_FOR_DELIVERY);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancel(orderId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("out for delivery");

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when order is failed")
        void testCancelOrderFailed() {
            OrderEntity order = buildSavedOrder(OrderStatus.FAILED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancel(orderId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("failed");

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Confirm Order Tests")
    class ConfirmOrderTests {

        @Test
        @DisplayName("Should confirm order successfully")
        void testConfirmOrderSuccess() {
            OrderEntity order = buildSavedOrder(OrderStatus.PENDING);
            StockReservedEvent event = StockReservedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);

            orderService.confirmOrder(event);

            verify(orderRepository, times(1)).save(argThat(o ->
                    o.getStatus() == OrderStatus.CONFIRMED &&
                            o.getConfirmedAt() != null
            ));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void testConfirmOrderNotFound() {
            StockReservedEvent event = StockReservedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.confirmOrder(event))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Mark As Shipped Tests")
    class MarkAsShippedTests {

        @Test
        @DisplayName("Should mark order as shipped successfully")
        void testMarkAsShippedSuccess() {
            OrderEntity order = buildSavedOrder(OrderStatus.CONFIRMED);
            ShipmentScheduledEvent event = ShipmentScheduledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);

            orderService.markAsShipped(event);

            verify(orderRepository, times(1)).save(argThat(o ->
                    o.getStatus() == OrderStatus.SHIPPED &&
                            o.getShippedAt() != null
            ));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void testMarkAsShippedNotFound() {
            ShipmentScheduledEvent event = ShipmentScheduledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.markAsShipped(event))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Mark As Out For Delivery Tests")
    class MarkAsOutForDeliveryTests {

        @Test
        @DisplayName("Should mark order as out for delivery successfully")
        void testMarkAsOutForDeliverySuccess() {
            OrderEntity order = buildSavedOrder(OrderStatus.SHIPPED);
            Instant dispatchedAt = Instant.now();
            ShipmentOutForDeliveryEvent event = ShipmentOutForDeliveryEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .dispatchedAt(dispatchedAt)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);

            orderService.markAsOutForDelivery(event);

            verify(orderRepository, times(1)).save(argThat(o ->
                    o.getStatus() == OrderStatus.OUT_FOR_DELIVERY &&
                            o.getDispatchedAt().equals(dispatchedAt)
            ));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void testMarkAsOutForDeliveryNotFound() {
            ShipmentOutForDeliveryEvent event = ShipmentOutForDeliveryEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .dispatchedAt(Instant.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.markAsOutForDelivery(event))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Mark As Delivered Tests")
    class MarkAsDeliveredTests {

        @Test
        @DisplayName("Should mark order as delivered successfully")
        void testMarkAsDeliveredSuccess() {
            OrderEntity order = buildSavedOrder(OrderStatus.OUT_FOR_DELIVERY);

            Instant deliveredAt = Instant.now();

            ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .deliveredAt(deliveredAt)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);

            orderService.markAsDelivered(event);

            verify(orderRepository, times(1)).save(argThat(o ->
                    o.getStatus() == OrderStatus.DELIVERED &&
                            o.getDeliveredAt().equals(deliveredAt)
            ));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void testMarkAsDeliveredNotFound() {
            ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .deliveredAt(Instant.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.markAsDelivered(event))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Mark As Failed Tests")
    class MarkAsFailedTests {

        @Test
        @DisplayName("Should mark order as failed successfully")
        void testMarkAsFailedSuccess() {
            OrderEntity order = buildSavedOrder(OrderStatus.OUT_FOR_DELIVERY);
            Instant failedAt = Instant.now();
            ShipmentFailedEvent event = ShipmentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .failureReason("Delivery address not found")
                    .failedAt(failedAt)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);

            orderService.markAsFailed(event);

            verify(orderRepository, times(1)).save(argThat(o ->
                    o.getStatus() == OrderStatus.FAILED &&
                            o.getFailedAt().equals(failedAt)
            ));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void testMarkAsFailedNotFound() {
            ShipmentFailedEvent event = ShipmentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .failureReason("Delivery address not found")
                    .failedAt(Instant.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.markAsFailed(event))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(orderRepository, never()).save(any(OrderEntity.class));
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should get order by id successfully")
        void testGetByIdSuccess() {
            OrderEntity order = buildSavedOrder(OrderStatus.PENDING);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OrderResponse result = orderService.getById(orderId);

            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found by id")
        void testGetByIdNotFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getById(orderId))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("Should return all orders successfully")
        void testGetAllSuccess() {
            OrderEntity order1 = buildSavedOrder(OrderStatus.PENDING);
            OrderEntity order2 = buildSavedOrder(OrderStatus.CONFIRMED);
            order2.setId(UUID.randomUUID());

            when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

            List<OrderResponse> result = orderService.getAll();

            assertThat(result).hasSize(2);
            verify(orderRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void testGetAllEmpty() {
            when(orderRepository.findAll()).thenReturn(List.of());

            List<OrderResponse> result = orderService.getAll();

            assertThat(result).isEmpty();
            verify(orderRepository, times(1)).findAll();
        }
    }
}