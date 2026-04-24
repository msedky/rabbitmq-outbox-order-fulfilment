package com.rabbitmqoutbox.orderservice.messaging.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.orderservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.orderservice.messaging.event.OrderCancelledEvent;
import com.rabbitmqoutbox.orderservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledPublishStrategy implements OutboxEventPublishStrategy {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "ORDER_CANCELLED";
    }

    @Override
    public void publish(OutboxEventEntity event) {
        try {
            OrderCancelledEvent orderCancelledEvent = objectMapper.readValue(
                    event.getPayload(), OrderCancelledEvent.class);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_CANCELLED_EXCHANGE,
                    RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
                    orderCancelledEvent);
            log.info("Published ORDER_CANCELLED event for orderId={}",
                    orderCancelledEvent.getOrderId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ORDER_CANCELLED payload", e);
        }
    }
}