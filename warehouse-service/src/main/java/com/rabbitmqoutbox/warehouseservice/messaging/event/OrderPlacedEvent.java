package com.rabbitmqoutbox.warehouseservice.messaging.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent {

    private String eventId;
    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderPlacedEventItem> items;
    private Instant occurredAt;
}