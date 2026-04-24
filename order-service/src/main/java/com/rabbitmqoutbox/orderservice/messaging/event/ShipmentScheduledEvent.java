package com.rabbitmqoutbox.orderservice.messaging.event;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentScheduledEvent {

    private String eventId;
    private UUID orderId;
    private UUID shipmentId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;
    private Instant scheduledAt;
    private Instant occurredAt;
}