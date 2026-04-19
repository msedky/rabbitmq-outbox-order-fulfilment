package com.rabbitmqoutbox.shippingservice.service.impl;

import com.rabbitmqoutbox.shippingservice.exception.ShipmentNotFoundException;
import com.rabbitmqoutbox.shippingservice.mapper.ShipmentMapper;
import com.rabbitmqoutbox.shippingservice.messaging.event.ShipmentScheduledEvent;
import com.rabbitmqoutbox.shippingservice.messaging.event.StockReservedEvent;
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