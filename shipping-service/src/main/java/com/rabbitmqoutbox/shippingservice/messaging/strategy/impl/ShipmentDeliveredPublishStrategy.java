package com.rabbitmqoutbox.shippingservice.messaging.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.shippingservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.shippingservice.messaging.event.ShipmentDeliveredEvent;
import com.rabbitmqoutbox.shippingservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentDeliveredPublishStrategy implements OutboxEventPublishStrategy {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "SHIPMENT_DELIVERED";
    }

    @Override
    public void publish(OutboxEventEntity event) {
        try {
            ShipmentDeliveredEvent shipmentDeliveredEvent = objectMapper.readValue(
                    event.getPayload(), ShipmentDeliveredEvent.class);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SHIPMENT_DELIVERED_EXCHANGE,
                    RabbitMQConfig.SHIPMENT_DELIVERED_ROUTING_KEY,
                    shipmentDeliveredEvent);
            log.info("Published SHIPMENT_DELIVERED event for orderId={}",
                    shipmentDeliveredEvent.getOrderId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize SHIPMENT_DELIVERED payload", e);
        }
    }
}