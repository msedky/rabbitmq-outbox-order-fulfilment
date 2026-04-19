package com.rabbitmqoutbox.shippingservice.model.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentResponse {

    private UUID shipmentId;
    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;
    private String status;
    private Instant scheduledAt;
    private Instant createdAt;
    private Instant updatedAt;
}