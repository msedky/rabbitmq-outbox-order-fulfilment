package com.rabbitmqoutbox.shippingservice.messaging.event;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservedEvent {

    private String eventId;
    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;
    private List<StockReservedEventItem> items;
    private Instant occurredAt;
}