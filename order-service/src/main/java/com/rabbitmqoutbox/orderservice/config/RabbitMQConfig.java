package com.rabbitmqoutbox.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Outbound — order-service publishes, warehouse-service consumes
    public static final String ORDER_PLACED_EXCHANGE = "ob.order.placed.exchange";
    public static final String ORDER_PLACED_ROUTING_KEY = "ob.order.placed";

    // Outbound — order-service publishes, warehouse-service consumes
    public static final String ORDER_CANCELLED_EXCHANGE = "ob.order.cancelled.exchange";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "ob.order.cancelled";

    // Inbound — warehouse-service publishes, order-service consumes
    public static final String STOCK_RESERVED_QUEUE = "ob.stock.reserved.order.queue";
    public static final String STOCK_RESERVED_EXCHANGE = "ob.stock.reserved.exchange";
    public static final String STOCK_RESERVED_ROUTING_KEY = "ob.stock.reserved";
    public static final String STOCK_RESERVED_DLX = "ob.stock.reserved.order.dlx";
    public static final String STOCK_RESERVED_DLQ = "ob.stock.reserved.order.dlq";

    // Inbound — shipping-service publishes, order-service consumes
    public static final String SHIPMENT_SCHEDULED_QUEUE = "ob.shipment.scheduled.order.queue";
    public static final String SHIPMENT_SCHEDULED_EXCHANGE = "ob.shipment.scheduled.exchange";
    public static final String SHIPMENT_SCHEDULED_ROUTING_KEY = "ob.shipment.scheduled";
    public static final String SHIPMENT_SCHEDULED_DLX = "ob.shipment.scheduled.order.dlx";
    public static final String SHIPMENT_SCHEDULED_DLQ = "ob.shipment.scheduled.order.dlq";

    // Inbound — shipping-service publishes, order-service consumes
    public static final String SHIPMENT_OUT_FOR_DELIVERY_QUEUE = "ob.shipment.out-for-delivery.order.queue";
    public static final String SHIPMENT_OUT_FOR_DELIVERY_EXCHANGE = "ob.shipment.out-for-delivery.exchange";
    public static final String SHIPMENT_OUT_FOR_DELIVERY_ROUTING_KEY = "ob.shipment.out-for-delivery";
    public static final String SHIPMENT_OUT_FOR_DELIVERY_DLX = "ob.shipment.out-for-delivery.order.dlx";
    public static final String SHIPMENT_OUT_FOR_DELIVERY_DLQ = "ob.shipment.out-for-delivery.order.dlq";

    // Inbound — shipping-service publishes, order-service consumes
    public static final String SHIPMENT_DELIVERED_QUEUE = "ob.shipment.delivered.order.queue";
    public static final String SHIPMENT_DELIVERED_EXCHANGE = "ob.shipment.delivered.exchange";
    public static final String SHIPMENT_DELIVERED_ROUTING_KEY = "ob.shipment.delivered";
    public static final String SHIPMENT_DELIVERED_DLX = "ob.shipment.delivered.order.dlx";
    public static final String SHIPMENT_DELIVERED_DLQ = "ob.shipment.delivered.order.dlq";

    // Inbound — shipping-service publishes, order-service consumes
    public static final String SHIPMENT_FAILED_QUEUE = "ob.shipment.failed.order.queue";
    public static final String SHIPMENT_FAILED_EXCHANGE = "ob.shipment.failed.exchange";
    public static final String SHIPMENT_FAILED_ROUTING_KEY = "ob.shipment.failed";
    public static final String SHIPMENT_FAILED_DLX = "ob.shipment.failed.order.dlx";
    public static final String SHIPMENT_FAILED_DLQ = "ob.shipment.failed.order.dlq";

    public static final String DEAD_LETTER_EXCHANGE_ARG = "x-dead-letter-exchange";

    // Outbound exchanges only
    @Bean
    public TopicExchange orderPlacedExchange() {
        return new TopicExchange(ORDER_PLACED_EXCHANGE);
    }

    @Bean
    public TopicExchange orderCancelledExchange() {
        return new TopicExchange(ORDER_CANCELLED_EXCHANGE);
    }

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

    // Inbound — shipment scheduled
    @Bean
    public TopicExchange shipmentScheduledExchange() {
        return new TopicExchange(SHIPMENT_SCHEDULED_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentScheduledDlx() {
        return new TopicExchange(SHIPMENT_SCHEDULED_DLX);
    }

    @Bean
    public Queue shipmentScheduledQueue() {
        return QueueBuilder.durable(SHIPMENT_SCHEDULED_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, SHIPMENT_SCHEDULED_DLX)
                .build();
    }

    @Bean
    public Queue shipmentScheduledDlq() {
        return QueueBuilder.durable(SHIPMENT_SCHEDULED_DLQ).build();
    }

    @Bean
    public Binding shipmentScheduledBinding(TopicExchange shipmentScheduledExchange,
                                            Queue shipmentScheduledQueue) {
        return BindingBuilder.bind(shipmentScheduledQueue)
                .to(shipmentScheduledExchange)
                .with(SHIPMENT_SCHEDULED_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentScheduledDlqBinding(TopicExchange shipmentScheduledDlx,
                                               Queue shipmentScheduledDlq) {
        return BindingBuilder.bind(shipmentScheduledDlq)
                .to(shipmentScheduledDlx)
                .with("#");
    }

    // Inbound — shipment out for delivery
    @Bean
    public TopicExchange shipmentOutForDeliveryExchange() {
        return new TopicExchange(SHIPMENT_OUT_FOR_DELIVERY_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentOutForDeliveryDlx() {
        return new TopicExchange(SHIPMENT_OUT_FOR_DELIVERY_DLX);
    }

    @Bean
    public Queue shipmentOutForDeliveryQueue() {
        return QueueBuilder.durable(SHIPMENT_OUT_FOR_DELIVERY_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, SHIPMENT_OUT_FOR_DELIVERY_DLX)
                .build();
    }

    @Bean
    public Queue shipmentOutForDeliveryDlq() {
        return QueueBuilder.durable(SHIPMENT_OUT_FOR_DELIVERY_DLQ).build();
    }

    @Bean
    public Binding shipmentOutForDeliveryBinding(TopicExchange shipmentOutForDeliveryExchange,
                                                 Queue shipmentOutForDeliveryQueue) {
        return BindingBuilder.bind(shipmentOutForDeliveryQueue)
                .to(shipmentOutForDeliveryExchange)
                .with(SHIPMENT_OUT_FOR_DELIVERY_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentOutForDeliveryDlqBinding(TopicExchange shipmentOutForDeliveryDlx,
                                                    Queue shipmentOutForDeliveryDlq) {
        return BindingBuilder.bind(shipmentOutForDeliveryDlq)
                .to(shipmentOutForDeliveryDlx)
                .with("#");
    }

    // Inbound — shipment delivered
    @Bean
    public TopicExchange shipmentDeliveredExchange() {
        return new TopicExchange(SHIPMENT_DELIVERED_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentDeliveredDlx() {
        return new TopicExchange(SHIPMENT_DELIVERED_DLX);
    }

    @Bean
    public Queue shipmentDeliveredQueue() {
        return QueueBuilder.durable(SHIPMENT_DELIVERED_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, SHIPMENT_DELIVERED_DLX)
                .build();
    }

    @Bean
    public Queue shipmentDeliveredDlq() {
        return QueueBuilder.durable(SHIPMENT_DELIVERED_DLQ).build();
    }

    @Bean
    public Binding shipmentDeliveredBinding(TopicExchange shipmentDeliveredExchange,
                                            Queue shipmentDeliveredQueue) {
        return BindingBuilder.bind(shipmentDeliveredQueue)
                .to(shipmentDeliveredExchange)
                .with(SHIPMENT_DELIVERED_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentDeliveredDlqBinding(TopicExchange shipmentDeliveredDlx,
                                               Queue shipmentDeliveredDlq) {
        return BindingBuilder.bind(shipmentDeliveredDlq)
                .to(shipmentDeliveredDlx)
                .with("#");
    }

    // Inbound — shipment failed
    @Bean
    public TopicExchange shipmentFailedExchange() {
        return new TopicExchange(SHIPMENT_FAILED_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentFailedDlx() {
        return new TopicExchange(SHIPMENT_FAILED_DLX);
    }

    @Bean
    public Queue shipmentFailedQueue() {
        return QueueBuilder.durable(SHIPMENT_FAILED_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARG, SHIPMENT_FAILED_DLX)
                .build();
    }

    @Bean
    public Queue shipmentFailedDlq() {
        return QueueBuilder.durable(SHIPMENT_FAILED_DLQ).build();
    }

    @Bean
    public Binding shipmentFailedBinding(TopicExchange shipmentFailedExchange,
                                         Queue shipmentFailedQueue) {
        return BindingBuilder.bind(shipmentFailedQueue)
                .to(shipmentFailedExchange)
                .with(SHIPMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentFailedDlqBinding(TopicExchange shipmentFailedDlx,
                                            Queue shipmentFailedDlq) {
        return BindingBuilder.bind(shipmentFailedDlq)
                .to(shipmentFailedDlx)
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