package com.rabbitmqoutbox.warehouseservice.config;

import com.rabbitmqoutbox.warehouseservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.warehouseservice.messaging.strategy.impl.StockReservedPublishStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OutboxPublishStrategyConfig {

    @Bean
    public Map<String, OutboxEventPublishStrategy> outboxPublishStrategyMap(
            StockReservedPublishStrategy stockReservedPublishStrategy) {

        return Map.of(
                stockReservedPublishStrategy.eventType(), stockReservedPublishStrategy
        );
    }
}