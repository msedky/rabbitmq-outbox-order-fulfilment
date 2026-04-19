package com.rabbitmqoutbox.warehouseservice.messaging.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservedEventItem {

    private String productId;
    private String productName;
    private Integer quantity;
}