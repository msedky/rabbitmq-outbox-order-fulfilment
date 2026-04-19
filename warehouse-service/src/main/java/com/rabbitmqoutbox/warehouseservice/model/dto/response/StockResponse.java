package com.rabbitmqoutbox.warehouseservice.model.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponse {

    private UUID stockId;
    private String productId;
    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Instant createdAt;
    private Instant updatedAt;
}