package com.rabbitmqoutbox.shippingservice.messaging.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.shippingservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.shippingservice.messaging.event.ShipmentFailedEvent;
import com.rabbitmqoutbox.shippingservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentFailedPublishStrategy implements OutboxEventPublishStrategy {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "SHIPMENT_FAILED";
    }

    @Override
    public void publish(OutboxEventEntity event) {
        try {
            ShipmentFailedEvent shipmentFailedEvent =
                    objectMapper.readValue(event.getPayload(),
                            ShipmentFailedEvent.class);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SHIPMENT_FAILED_EXCHANGE,
                    RabbitMQConfig.SHIPMENT_FAILED_ROUTING_KEY,
                    shipmentFailedEvent);
            log.info("Published SHIPMENT_FAILED event for orderId={}",
                    shipmentFailedEvent.getOrderId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize SHIPMENT_FAILED payload", e);
        }
    }
}