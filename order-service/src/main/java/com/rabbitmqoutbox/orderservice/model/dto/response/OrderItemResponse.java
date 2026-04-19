package com.rabbitmqoutbox.orderservice.model.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID itemId;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}