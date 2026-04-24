package com.rabbitmqoutbox.orderservice.model.dto.response;

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
public class OrderResponse {

    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private List<OrderItemResponse> items;
    private Instant confirmedAt;
    private Instant shippedAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Instant failedAt;
    private Instant createdAt;
    private Instant updatedAt;
}