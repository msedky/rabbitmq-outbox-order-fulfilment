package com.rabbitmqoutbox.shippingservice.config;

import com.rabbitmqoutbox.shippingservice.messaging.strategy.OutboxEventPublishStrategy;
import com.rabbitmqoutbox.shippingservice.messaging.strategy.impl.ShipmentDeliveredPublishStrategy;
import com.rabbitmqoutbox.shippingservice.messaging.strategy.impl.ShipmentFailedPublishStrategy;
import com.rabbitmqoutbox.shippingservice.messaging.strategy.impl.ShipmentOutForDeliveryPublishStrategy;
import com.rabbitmqoutbox.shippingservice.messaging.strategy.impl.ShipmentScheduledPublishStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OutboxPublishStrategyConfig {

    @Bean
    public Map<String, OutboxEventPublishStrategy> outboxPublishStrategyMap(
            ShipmentScheduledPublishStrategy shipmentScheduledPublishStrategy,
            ShipmentOutForDeliveryPublishStrategy shipmentOutForDeliveryPublishStrategy,
            ShipmentDeliveredPublishStrategy shipmentDeliveredPublishStrategy,
            ShipmentFailedPublishStrategy shipmentFailedPublishStrategy) {

        return Map.of(
                shipmentScheduledPublishStrategy.eventType(), shipmentScheduledPublishStrategy,
                shipmentOutForDeliveryPublishStrategy.eventType(), shipmentOutForDeliveryPublishStrategy,
                shipmentDeliveredPublishStrategy.eventType(), shipmentDeliveredPublishStrategy,
                shipmentFailedPublishStrategy.eventType(), shipmentFailedPublishStrategy
        );
    }
}