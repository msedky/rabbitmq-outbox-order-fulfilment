package com.rabbitmqoutbox.warehouseservice.model.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationResponse {

    private UUID reservationId;
    private UUID orderId;
    private String productId;
    private String productName;
    private Integer quantity;
    private String status;
    private Instant createdAt;
}