package com.rabbitmqoutbox.shippingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Inbound — consuming from warehouse-service
    public static final String STOCK_RESERVED_EXCHANGE = "stock.reserved.exchange";
    public static final String STOCK_RESERVED_QUEUE = "stock.reserved.queue";
    public static final String STOCK_RESERVED_ROUTING_KEY = "stock.reserved";

    // Outbound — publishing after shipment scheduled
    public static final String SHIPMENT_SCHEDULED_EXCHANGE = "shipment.scheduled.exchange";
    public static final String SHIPMENT_SCHEDULED_QUEUE = "shipment.scheduled.queue";
    public static final String SHIPMENT_SCHEDULED_ROUTING_KEY = "shipment.scheduled";

    // Inbound
    @Bean
    public TopicExchange stockReservedExchange() {
        return new TopicExchange(STOCK_RESERVED_EXCHANGE);
    }

    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(STOCK_RESERVED_QUEUE).build();
    }

    @Bean
    public Binding stockReservedBinding() {
        return BindingBuilder
                .bind(stockReservedQueue())
                .to(stockReservedExchange())
                .with(STOCK_RESERVED_ROUTING_KEY);
    }

    // Outbound
    @Bean
    public TopicExchange shipmentScheduledExchange() {
        return new TopicExchange(SHIPMENT_SCHEDULED_EXCHANGE);
    }

    @Bean
    public Queue shipmentScheduledQueue() {
        return QueueBuilder.durable(SHIPMENT_SCHEDULED_QUEUE).build();
    }

    @Bean
    public Binding shipmentScheduledBinding() {
        return BindingBuilder
                .bind(shipmentScheduledQueue())
                .to(shipmentScheduledExchange())
                .with(SHIPMENT_SCHEDULED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}