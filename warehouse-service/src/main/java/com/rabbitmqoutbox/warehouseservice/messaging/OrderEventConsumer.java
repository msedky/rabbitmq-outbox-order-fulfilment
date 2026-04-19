package com.rabbitmqoutbox.warehouseservice.messaging;

import com.rabbitmqoutbox.warehouseservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.warehouseservice.messaging.event.OrderPlacedEvent;
import com.rabbitmqoutbox.warehouseservice.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final StockService stockService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received ORDER_PLACED event for orderId={}", event.getOrderId());
        stockService.reserveStock(event);
    }
}