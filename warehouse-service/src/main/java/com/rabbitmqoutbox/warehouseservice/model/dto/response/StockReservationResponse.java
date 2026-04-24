package com.rabbitmqoutbox.warehouseservice.model.dto.response;

import com.rabbitmqoutbox.warehouseservice.model.enums.ReservationStatus;
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
    private ReservationStatus status;
    private Integer quantityBeforeReservation;
    private Integer quantityAfterReservation;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant releasedAt;
    private Instant fulfilledAt;
}