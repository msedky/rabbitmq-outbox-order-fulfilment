package com.rabbitmqoutbox.shippingservice.service.impl;

import com.rabbitmqoutbox.shippingservice.exception.ShipmentNotFoundException;
import com.rabbitmqoutbox.shippingservice.mapper.ShipmentMapper;
import com.rabbitmqoutbox.shippingservice.mapper.ShipmentMapperImpl;
import com.rabbitmqoutbox.shippingservice.messaging.event.*;
import com.rabbitmqoutbox.shippingservice.model.dto.response.ShipmentResponse;
import com.rabbitmqoutbox.shippingservice.model.entity.ShipmentEntity;
import com.rabbitmqoutbox.shippingservice.model.enums.ShipmentStatus;
import com.rabbitmqoutbox.shippingservice.repository.ShipmentRepository;
import com.rabbitmqoutbox.shippingservice.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentServiceImpl Unit Tests")
class ShipmentServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OutboxEventService outboxEventService;

    private ShipmentMapper shipmentMapper;
    private ShipmentServiceImpl shipmentService;

    private UUID shipmentId;
    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerId = "CST-001";
        customerEmail = "customer@email.com";
        deliveryAddress = "123 Main St";

        shipmentMapper = new ShipmentMapperImpl();
        shipmentService = new ShipmentServiceImpl(
                shipmentRepository, outboxEventService, shipmentMapper);
    }

    private ShipmentEntity buildShipment(ShipmentStatus status) {
        return ShipmentEntity.builder()
                .id(shipmentId)
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .deliveryAddress(deliveryAddress)
                .status(status)
                .scheduledAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private StockReservedEvent buildStockReservedEvent() {
        return StockReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .deliveryAddress(deliveryAddress)
                .items(List.of())
                .occurredAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Schedule Shipment Tests")
    class ScheduleShipmentTests {

        @Test
        @DisplayName("Should schedule shipment successfully")
        void testScheduleShipmentSuccess() {
            StockReservedEvent event = buildStockReservedEvent();
            ShipmentEntity savedShipment = buildShipment(ShipmentStatus.SCHEDULED);

            when(shipmentRepository.save(any(ShipmentEntity.class)))
                    .thenReturn(savedShipment);

            shipmentService.scheduleShipment(event);

            ArgumentCaptor<ShipmentEntity> shipmentCaptor =
                    ArgumentCaptor.forClass(ShipmentEntity.class);

            verify(shipmentRepository, times(1)).save(shipmentCaptor.capture());

            ShipmentEntity capturedShipment = shipmentCaptor.getValue();
            assertThat(capturedShipment.getOrderId()).isEqualTo(orderId);
            assertThat(capturedShipment.getCustomerId()).isEqualTo(customerId);
            assertThat(capturedShipment.getCustomerEmail()).isEqualTo(customerEmail);
            assertThat(capturedShipment.getDeliveryAddress()).isEqualTo(deliveryAddress);
            assertThat(capturedShipment.getStatus()).isEqualTo(ShipmentStatus.SCHEDULED);
            assertThat(capturedShipment.getScheduledAt()).isNotNull();

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("SHIPMENT"),
                    eq("SHIPMENT_SCHEDULED"),
                    eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue()).isInstanceOf(ShipmentScheduledEvent.class);
            ShipmentScheduledEvent capturedEvent =
                    (ShipmentScheduledEvent) eventCaptor.getValue();
            assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(capturedEvent.getShipmentId()).isEqualTo(shipmentId);
        }
    }

    @Nested
    @DisplayName("Dispatch Shipment Tests")
    class DispatchShipmentTests {

        @Test
        @DisplayName("Should dispatch shipment successfully")
        void testDispatchSuccess() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.SCHEDULED);
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
            when(shipmentRepository.save(shipment)).thenReturn(shipment);

            ShipmentResponse result = shipmentService.dispatch(shipmentId);

            assertThat(result).isNotNull();
            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);
            assertThat(shipment.getDispatchedAt()).isNotNull();

            verify(shipmentRepository, times(1)).save(shipment);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("SHIPMENT"),
                    eq("SHIPMENT_OUT_FOR_DELIVERY"),
                    eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue()).isInstanceOf(ShipmentOutForDeliveryEvent.class);
            ShipmentOutForDeliveryEvent capturedEvent =
                    (ShipmentOutForDeliveryEvent) eventCaptor.getValue();
            assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(capturedEvent.getShipmentId()).isEqualTo(shipmentId);
            assertThat(capturedEvent.getDispatchedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw ShipmentNotFoundException when shipment not found")
        void testDispatchNotFound() {
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shipmentService.dispatch(shipmentId))
                    .isInstanceOf(ShipmentNotFoundException.class);

            verify(shipmentRepository, never()).save(any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when shipment is not SCHEDULED")
        void testDispatchInvalidStatus() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.OUT_FOR_DELIVERY);
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

            assertThatThrownBy(() -> shipmentService.dispatch(shipmentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot dispatch");

            verify(shipmentRepository, never()).save(any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Deliver Shipment Tests")
    class DeliverShipmentTests {

        @Test
        @DisplayName("Should deliver shipment successfully")
        void testDeliverSuccess() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.OUT_FOR_DELIVERY);
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
            when(shipmentRepository.save(shipment)).thenReturn(shipment);

            ShipmentResponse result = shipmentService.deliver(shipmentId);

            assertThat(result).isNotNull();
            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
            assertThat(shipment.getDeliveredAt()).isNotNull();

            verify(shipmentRepository, times(1)).save(shipment);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("SHIPMENT"),
                    eq("SHIPMENT_DELIVERED"),
                    eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue()).isInstanceOf(ShipmentDeliveredEvent.class);
            ShipmentDeliveredEvent capturedEvent =
                    (ShipmentDeliveredEvent) eventCaptor.getValue();
            assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(capturedEvent.getShipmentId()).isEqualTo(shipmentId);
            assertThat(capturedEvent.getDeliveredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw ShipmentNotFoundException when shipment not found")
        void testDeliverNotFound() {
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shipmentService.deliver(shipmentId))
                    .isInstanceOf(ShipmentNotFoundException.class);

            verify(shipmentRepository, never()).save(any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when shipment is not OUT_FOR_DELIVERY")
        void testDeliverInvalidStatus() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.SCHEDULED);
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

            assertThatThrownBy(() -> shipmentService.deliver(shipmentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot deliver");

            verify(shipmentRepository, never()).save(any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Fail Shipment Tests")
    class FailShipmentTests {

        @Test
        @DisplayName("Should fail shipment successfully")
        void testFailSuccess() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.SCHEDULED);
            String failureReason = "Delivery address not found";

            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
            when(shipmentRepository.save(shipment)).thenReturn(shipment);

            ShipmentResponse result = shipmentService.fail(shipmentId, failureReason);

            assertThat(result).isNotNull();
            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.FAILED);
            assertThat(shipment.getFailedAt()).isNotNull();

            verify(shipmentRepository, times(1)).save(shipment);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("SHIPMENT"),
                    eq("SHIPMENT_FAILED"),
                    eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue()).isInstanceOf(ShipmentFailedEvent.class);
            ShipmentFailedEvent capturedEvent =
                    (ShipmentFailedEvent) eventCaptor.getValue();
            assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(capturedEvent.getShipmentId()).isEqualTo(shipmentId);
            assertThat(capturedEvent.getFailureReason()).isEqualTo(failureReason);
            assertThat(capturedEvent.getFailedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw ShipmentNotFoundException when shipment not found")
        void testFailNotFound() {
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shipmentService.fail(shipmentId, "reason"))
                    .isInstanceOf(ShipmentNotFoundException.class);

            verify(shipmentRepository, never()).save(any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when shipment is already delivered")
        void testFailDeliveredShipment() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.DELIVERED);
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

            assertThatThrownBy(() -> shipmentService.fail(shipmentId, "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot fail a delivered shipment");

            verify(shipmentRepository, never()).save(any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when shipment is already failed")
        void testFailAlreadyFailed() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.FAILED);
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

            assertThatThrownBy(() -> shipmentService.fail(shipmentId, "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already failed");

            verify(shipmentRepository, never()).save(any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Cancel Shipment Tests")
    class CancelShipmentTests {

        @Test
        @DisplayName("Should cancel shipment successfully")
        void testCancelSuccess() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.SCHEDULED);
            when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
            when(shipmentRepository.save(shipment)).thenReturn(shipment);

            shipmentService.cancelShipment(orderId);

            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
            assertThat(shipment.getCancelledAt()).isNotNull();

            verify(shipmentRepository, times(1)).save(shipment);
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw ShipmentNotFoundException when shipment not found")
        void testCancelNotFound() {
            when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shipmentService.cancelShipment(orderId))
                    .isInstanceOf(ShipmentNotFoundException.class);

            verify(shipmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when shipment is delivered")
        void testCancelDeliveredShipment() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.DELIVERED);
            when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

            assertThatThrownBy(() -> shipmentService.cancelShipment(orderId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel a delivered shipment");

            verify(shipmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return early when shipment is already cancelled")
        void testCancelAlreadyCancelled() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.CANCELLED);
            when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

            shipmentService.cancelShipment(orderId);

            verify(shipmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Shipment Tests")
    class GetShipmentTests {

        @Test
        @DisplayName("Should get shipment by id successfully")
        void testGetByIdSuccess() {
            ShipmentEntity shipment = buildShipment(ShipmentStatus.SCHEDULED);
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

            ShipmentResponse result = shipmentService.getById(shipmentId);

            assertThat(result).isNotNull();
            assertThat(result.getShipmentId()).isEqualTo(shipmentId);
            assertThat(result.getOrderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("Should throw ShipmentNotFoundException when shipment not found by id")
        void testGetByIdNotFound() {
            when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shipmentService.getById(shipmentId))
                    .isInstanceOf(ShipmentNotFoundException.class);
        }

        @Test
        @DisplayName("Should return all shipments successfully")
        void testGetAllSuccess() {
            ShipmentEntity shipment1 = buildShipment(ShipmentStatus.SCHEDULED);
            ShipmentEntity shipment2 = buildShipment(ShipmentStatus.DELIVERED);
            shipment2.setId(UUID.randomUUID());

            when(shipmentRepository.findAll()).thenReturn(List.of(shipment1, shipment2));

            List<ShipmentResponse> result = shipmentService.getAll();

            assertThat(result).hasSize(2);
            verify(shipmentRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no shipments exist")
        void testGetAllEmpty() {
            when(shipmentRepository.findAll()).thenReturn(List.of());

            List<ShipmentResponse> result = shipmentService.getAll();

            assertThat(result).isEmpty();
            verify(shipmentRepository, times(1)).findAll();
        }
    }
}