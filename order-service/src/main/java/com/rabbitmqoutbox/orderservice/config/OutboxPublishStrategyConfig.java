package com.rabbitmqoutbox.orderservice.config;

import com.rabbitmqoutbox.orderservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.orderservice.messaging.strategy.impl.OrderCancelledPublishStrategy;
import com.rabbitmqoutbox.orderservice.messaging.strategy.impl.OrderPlacedPublishStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OutboxPublishStrategyConfig {

    @Bean
    public Map<String, OutboxEventPublishStrategy> outboxPublishStrategyMap(
            OrderPlacedPublishStrategy orderPlacedPublishStrategy,
            OrderCancelledPublishStrategy orderCancelledPublishStrategy) {

        return Map.of(
                orderPlacedPublishStrategy.eventType(), orderPlacedPublishStrategy,
                orderCancelledPublishStrategy.eventType(), orderCancelledPublishStrategy
        );
    }
}