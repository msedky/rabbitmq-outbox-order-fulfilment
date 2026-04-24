package com.rabbitmqoutbox.warehouseservice.messaging.event;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentOutForDeliveryEvent {

    private String eventId;
    private UUID orderId;
    private UUID shipmentId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;
    private Instant dispatchedAt;
    private Instant occurredAt;
}