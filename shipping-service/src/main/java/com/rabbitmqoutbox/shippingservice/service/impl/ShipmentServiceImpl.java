package com.rabbitmqoutbox.shippingservice.service.impl;

import com.rabbitmqoutbox.shippingservice.exception.ShipmentNotFoundException;
import com.rabbitmqoutbox.shippingservice.mapper.ShipmentMapper;
import com.rabbitmqoutbox.shippingservice.messaging.event.*;
import com.rabbitmqoutbox.shippingservice.model.dto.response.ShipmentResponse;
import com.rabbitmqoutbox.shippingservice.model.entity.ShipmentEntity;
import com.rabbitmqoutbox.shippingservice.model.enums.ShipmentStatus;
import com.rabbitmqoutbox.shippingservice.repository.ShipmentRepository;
import com.rabbitmqoutbox.shippingservice.service.OutboxEventService;
import com.rabbitmqoutbox.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OutboxEventService outboxEventService;
    private final ShipmentMapper shipmentMapper;

    @Override
    @Transactional
    public void scheduleShipment(StockReservedEvent event) {

        ShipmentEntity shipment = ShipmentEntity.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .customerEmail(event.getCustomerEmail())
                .deliveryAddress(event.getDeliveryAddress())
                .status(ShipmentStatus.SCHEDULED)
                .scheduledAt(Instant.now())
                .build();

        ShipmentEntity savedShipment = shipmentRepository.save(shipment);

        log.info("Shipment scheduled for orderId={}, shipmentId={}",
                event.getOrderId(), savedShipment.getId());

        ShipmentScheduledEvent shipmentScheduledEvent = ShipmentScheduledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(savedShipment.getOrderId())
                .shipmentId(savedShipment.getId())
                .customerId(savedShipment.getCustomerId())
                .customerEmail(savedShipment.getCustomerEmail())
                .deliveryAddress(savedShipment.getDeliveryAddress())
                .scheduledAt(savedShipment.getScheduledAt())
                .occurredAt(Instant.now())
                .build();

        outboxEventService.saveEvent(
                savedShipment.getOrderId().toString(),
                "SHIPMENT",
                "SHIPMENT_SCHEDULED",
                shipmentScheduledEvent
        );

        log.info("Outbox event saved for SHIPMENT_SCHEDULED, orderId={}",
                event.getOrderId());
    }

    @Override
    @Transactional
    public ShipmentResponse dispatch(UUID shipmentId) {

        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found with id: " + shipmentId));

        if (shipment.getStatus() != ShipmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Cannot dispatch a shipment with status: " + shipment.getStatus());
        }

        Instant dispatchedAt = Instant.now();
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setDispatchedAt(dispatchedAt);
        ShipmentEntity savedShipment = shipmentRepository.save(shipment);

        log.info("Shipment dispatched for shipmentId={}, orderId={}",
                shipmentId, savedShipment.getOrderId());

        ShipmentOutForDeliveryEvent shipmentOutForDeliveryEvent =
                ShipmentOutForDeliveryEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(savedShipment.getOrderId())
                        .shipmentId(savedShipment.getId())
                        .customerId(savedShipment.getCustomerId())
                        .customerEmail(savedShipment.getCustomerEmail())
                        .deliveryAddress(savedShipment.getDeliveryAddress())
                        .dispatchedAt(dispatchedAt)
                        .occurredAt(Instant.now())
                        .build();

        outboxEventService.saveEvent(
                savedShipment.getOrderId().toString(),
                "SHIPMENT",
                "SHIPMENT_OUT_FOR_DELIVERY",
                shipmentOutForDeliveryEvent
        );

        log.info("Outbox event saved for SHIPMENT_OUT_FOR_DELIVERY, shipmentId={}",
                shipmentId);

        return shipmentMapper.toResponse(savedShipment);
    }

    @Override
    @Transactional
    public ShipmentResponse deliver(UUID shipmentId) {

        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found with id: " + shipmentId));

        if (shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException(
                    "Cannot deliver a shipment with status: " + shipment.getStatus());
        }

        Instant deliveredAt = Instant.now();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(deliveredAt);
        ShipmentEntity savedShipment = shipmentRepository.save(shipment);

        log.info("Shipment delivered for shipmentId={}, orderId={}",
                shipmentId, savedShipment.getOrderId());

        ShipmentDeliveredEvent shipmentDeliveredEvent = ShipmentDeliveredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(savedShipment.getOrderId())
                .shipmentId(savedShipment.getId())
                .customerId(savedShipment.getCustomerId())
                .customerEmail(savedShipment.getCustomerEmail())
                .deliveredAt(deliveredAt)
                .occurredAt(Instant.now())
                .build();

        outboxEventService.saveEvent(
                savedShipment.getOrderId().toString(),
                "SHIPMENT",
                "SHIPMENT_DELIVERED",
                shipmentDeliveredEvent
        );

        log.info("Outbox event saved for SHIPMENT_DELIVERED, shipmentId={}", shipmentId);

        return shipmentMapper.toResponse(savedShipment);
    }

    @Override
    @Transactional
    public ShipmentResponse fail(UUID shipmentId, String failureReason) {

        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found with id: " + shipmentId));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new IllegalStateException("Cannot fail a delivered shipment");
        }

        if (shipment.getStatus() == ShipmentStatus.FAILED) {
            throw new IllegalStateException("Shipment is already failed");
        }

        Instant failedAt = Instant.now();
        shipment.setStatus(ShipmentStatus.FAILED);
        shipment.setFailedAt(failedAt);
        ShipmentEntity savedShipment = shipmentRepository.save(shipment);

        log.info("Shipment failed for shipmentId={}, orderId={}",
                shipmentId, savedShipment.getOrderId());

        ShipmentFailedEvent shipmentFailedEvent = ShipmentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(savedShipment.getOrderId())
                .shipmentId(savedShipment.getId())
                .customerId(savedShipment.getCustomerId())
                .customerEmail(savedShipment.getCustomerEmail())
                .failureReason(failureReason)
                .failedAt(failedAt)
                .occurredAt(Instant.now())
                .build();

        outboxEventService.saveEvent(
                savedShipment.getOrderId().toString(),
                "SHIPMENT",
                "SHIPMENT_FAILED",
                shipmentFailedEvent
        );

        log.info("Outbox event saved for SHIPMENT_FAILED, shipmentId={}", shipmentId);

        return shipmentMapper.toResponse(savedShipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getById(UUID shipmentId) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found with id: " + shipmentId));
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAll() {
        return shipmentMapper.toResponseList(shipmentRepository.findAll());
    }
}