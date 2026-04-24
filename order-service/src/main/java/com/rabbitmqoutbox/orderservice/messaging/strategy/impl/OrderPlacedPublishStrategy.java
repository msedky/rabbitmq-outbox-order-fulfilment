package com.rabbitmqoutbox.orderservice.messaging.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.orderservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.orderservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.orderservice.messaging.event.OrderPlacedEvent;
import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedPublishStrategy implements OutboxEventPublishStrategy {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "ORDER_PLACED";
    }

    @Override
    public void publish(OutboxEventEntity event) {
        try {
            OrderPlacedEvent orderPlacedEvent = objectMapper.readValue(
                    event.getPayload(), OrderPlacedEvent.class);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_PLACED_EXCHANGE,
                    RabbitMQConfig.ORDER_PLACED_ROUTING_KEY,
                    orderPlacedEvent);
            log.info("Published ORDER_PLACED event for orderId={}",
                    orderPlacedEvent.getOrderId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ORDER_PLACED payload", e);
        }
    }
}