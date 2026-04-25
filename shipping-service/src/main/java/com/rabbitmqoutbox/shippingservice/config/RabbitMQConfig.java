package com.rabbitmqoutbox.shippingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Inbound — warehouse-service publishes, shipping-service consumes
    public static final String STOCK_RESERVED_QUEUE = "ob.stock.reserved.shipping.queue";
    public static final String STOCK_RESERVED_EXCHANGE = "ob.stock.reserved.exchange";
    public static final String STOCK_RESERVED_ROUTING_KEY = "ob.stock.reserved";
    public static final String STOCK_RESERVED_DLX = "ob.stock.reserved.shipping.dlx";
    public static final String STOCK_RESERVED_DLQ = "ob.stock.reserved.shipping.dlq";

    // Outbound exchanges only
    public static final String SHIPMENT_SCHEDULED_EXCHANGE = "ob.shipment.scheduled.exchange";
    public static final String SHIPMENT_SCHEDULED_ROUTING_KEY = "ob.shipment.scheduled";

    public static final String SHIPMENT_OUT_FOR_DELIVERY_EXCHANGE = "ob.shipment.out-for-delivery.exchange";
    public static final String SHIPMENT_OUT_FOR_DELIVERY_ROUTING_KEY = "ob.shipment.out-for-delivery";

    public static final String SHIPMENT_DELIVERED_EXCHANGE = "ob.shipment.delivered.exchange";
    public static final String SHIPMENT_DELIVERED_ROUTING_KEY = "ob.shipment.delivered";

    public static final String SHIPMENT_FAILED_EXCHANGE = "ob.shipment.failed.exchange";
    public static final String SHIPMENT_FAILED_ROUTING_KEY = "ob.shipment.failed";

    public static final String ORDER_CANCELLED_QUEUE = "ob.order.cancelled.shipping.queue";
    public static final String ORDER_CANCELLED_EXCHANGE = "ob.order.cancelled.exchange";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "ob.order.cancelled";
    public static final String ORDER_CANCELLED_DLX = "ob.order.cancelled.shipping.dlx";
    public static final String ORDER_CANCELLED_DLQ = "ob.order.cancelled.shipping.dlq";

    public static final String DEAD_LETTER_EXCHANGE_ARG = "x-dead-letter-exchange";

    // Inbound — stock reserved
    @Bean
    public TopicExchange stockReservedExchange() {
        return new TopicExchange(STOCK_RESERVED_EXCHANGE);
    }

    @Bean
    public TopicExchange stockReservedDlx() {
        return new TopicExchange(STOCK_RESERVED_DLX);
    }

    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(STOCK_RESERVED_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, STOCK_RESERVED_DLX)
                .build();
    }

    @Bean
    public Queue stockReservedDlq() {
        return QueueBuilder.durable(STOCK_RESERVED_DLQ).build();
    }

    @Bean
    public Binding stockReservedBinding(TopicExchange stockReservedExchange,
                                        Queue stockReservedQueue) {
        return BindingBuilder.bind(stockReservedQueue)
                .to(stockReservedExchange)
                .with(STOCK_RESERVED_ROUTING_KEY);
    }

    @Bean
    public Binding stockReservedDlqBinding(TopicExchange stockReservedDlx,
                                           Queue stockReservedDlq) {
        return BindingBuilder.bind(stockReservedDlq)
                .to(stockReservedDlx)
                .with("#");
    }

    // Outbound exchanges only
    @Bean
    public TopicExchange shipmentScheduledExchange() {
        return new TopicExchange(SHIPMENT_SCHEDULED_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentOutForDeliveryExchange() {
        return new TopicExchange(SHIPMENT_OUT_FOR_DELIVERY_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentDeliveredExchange() {
        return new TopicExchange(SHIPMENT_DELIVERED_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentFailedExchange() {
        return new TopicExchange(SHIPMENT_FAILED_EXCHANGE);
    }

    @Bean
    public TopicExchange orderCancelledExchange() {
        return new TopicExchange(ORDER_CANCELLED_EXCHANGE);
    }

    @Bean
    public TopicExchange orderCancelledDlx() {
        return new TopicExchange(ORDER_CANCELLED_DLX);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, ORDER_CANCELLED_DLX)
                .build();
    }

    @Bean
    public Queue orderCancelledDlq() {
        return QueueBuilder.durable(ORDER_CANCELLED_DLQ).build();
    }

    @Bean
    public Binding orderCancelledBinding(TopicExchange orderCancelledExchange,
                                         Queue orderCancelledQueue) {
        return BindingBuilder.bind(orderCancelledQueue)
                .to(orderCancelledExchange)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledDlqBinding(TopicExchange orderCancelledDlx,
                                            Queue orderCancelledDlq) {
        return BindingBuilder.bind(orderCancelledDlq)
                .to(orderCancelledDlx)
                .with("#");
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}