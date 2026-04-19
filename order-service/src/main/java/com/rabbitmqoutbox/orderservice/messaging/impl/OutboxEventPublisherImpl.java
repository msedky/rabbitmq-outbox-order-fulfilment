package com.rabbitmqoutbox.orderservice.messaging.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.orderservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.orderservice.messaging.OutboxEventPublisher;
import com.rabbitmqoutbox.orderservice.messaging.event.OrderPlacedEvent;
import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;
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
            if ("ORDER_PLACED".equals(event.getEventType())) {
                OrderPlacedEvent orderPlacedEvent = objectMapper.readValue(
                        event.getPayload(),
                        OrderPlacedEvent.class
                );
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_PLACED_EXCHANGE,
                        RabbitMQConfig.ORDER_PLACED_ROUTING_KEY,
                        orderPlacedEvent
                );
                log.info("Published ORDER_PLACED event for orderId={}",
                        orderPlacedEvent.getOrderId());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize outbox event payload", e);
        }
    }
}