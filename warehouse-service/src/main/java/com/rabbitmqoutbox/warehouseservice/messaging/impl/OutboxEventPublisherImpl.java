package com.rabbitmqoutbox.warehouseservice.messaging.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.warehouseservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.warehouseservice.messaging.OutboxEventPublisher;
import com.rabbitmqoutbox.warehouseservice.messaging.event.StockReservedEvent;
import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;
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
            if ("STOCK_RESERVED".equals(event.getEventType())) {
                StockReservedEvent stockReservedEvent = objectMapper.readValue(
                        event.getPayload(),
                        StockReservedEvent.class
                );
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.STOCK_RESERVED_EXCHANGE,
                        RabbitMQConfig.STOCK_RESERVED_ROUTING_KEY,
                        stockReservedEvent
                );
                log.info("Published STOCK_RESERVED event for orderId={}",
                        stockReservedEvent.getOrderId());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize outbox event payload", e);
        }
    }
}