package com.rabbitmqoutbox.shippingservice.messaging.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.shippingservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.shippingservice.messaging.event.ShipmentOutForDeliveryEvent;
import com.rabbitmqoutbox.shippingservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentOutForDeliveryPublishStrategy implements OutboxEventPublishStrategy {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "SHIPMENT_OUT_FOR_DELIVERY";
    }

    @Override
    public void publish(OutboxEventEntity event) {
        try {
            ShipmentOutForDeliveryEvent shipmentOutForDeliveryEvent =
                    objectMapper.readValue(event.getPayload(),
                            ShipmentOutForDeliveryEvent.class);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SHIPMENT_OUT_FOR_DELIVERY_EXCHANGE,
                    RabbitMQConfig.SHIPMENT_OUT_FOR_DELIVERY_ROUTING_KEY,
                    shipmentOutForDeliveryEvent);
            log.info("Published SHIPMENT_OUT_FOR_DELIVERY event for orderId={}",
                    shipmentOutForDeliveryEvent.getOrderId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize SHIPMENT_OUT_FOR_DELIVERY payload", e);
        }
    }
}