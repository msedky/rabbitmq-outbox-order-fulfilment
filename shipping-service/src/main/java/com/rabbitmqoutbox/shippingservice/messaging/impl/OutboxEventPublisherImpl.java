package com.rabbitmqoutbox.shippingservice.messaging.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.shippingservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.shippingservice.messaging.OutboxEventPublisher;
import com.rabbitmqoutbox.shippingservice.messaging.event.ShipmentScheduledEvent;
import com.rabbitmqoutbox.shippingservice.model.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherImpl implements OutboxEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(OutboxEventEntity event) {
        try {
            if ("SHIPMENT_SCHEDULED".equals(event.getEventType())) {
                ShipmentScheduledEvent shipmentScheduledEvent = objectMapper.readValue(
                        event.getPayload(),
                        ShipmentScheduledEvent.class
                );
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.SHIPMENT_SCHEDULED_EXCHANGE,
                        RabbitMQConfig.SHIPMENT_SCHEDULED_ROUTING_KEY,
                        shipmentScheduledEvent
                );
                log.info("Published SHIPMENT_SCHEDULED event for orderId={}",
                        shipmentScheduledEvent.getOrderId());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize outbox event payload", e);
        }
    }
}