package com.rabbitmqoutbox.shippingservice.messaging;

import com.rabbitmqoutbox.shippingservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.shippingservice.messaging.event.StockReservedEvent;
import com.rabbitmqoutbox.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {

    private final ShipmentService shipmentService;

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVED_QUEUE)
    public void handleStockReserved(StockReservedEvent event) {
        log.info("Received STOCK_RESERVED event for orderId={}", event.getOrderId());
        shipmentService.scheduleShipment(event);
    }
}