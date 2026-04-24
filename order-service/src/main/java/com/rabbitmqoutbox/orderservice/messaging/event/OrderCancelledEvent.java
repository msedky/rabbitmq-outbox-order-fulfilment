package com.rabbitmqoutbox.orderservice.messaging.event;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelledEvent {

    private String eventId;
    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private Instant occurredAt;
}