package com.rabbitmqoutbox.warehouseservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Inbound — order-service publishes, warehouse-service consumes
    public static final String ORDER_PLACED_QUEUE = "ob.order.placed.queue";
    public static final String ORDER_PLACED_EXCHANGE = "ob.order.placed.exchange";
    public static final String ORDER_PLACED_ROUTING_KEY = "ob.order.placed";
    public static final String ORDER_PLACED_DLX = "ob.order.placed.dlx";
    public static final String ORDER_PLACED_DLQ = "ob.order.placed.dlq";

    // Inbound — order-service publishes, warehouse-service consumes
    public static final String ORDER_CANCELLED_QUEUE = "ob.order.cancelled.warehouse.queue";
    public static final String ORDER_CANCELLED_EXCHANGE = "ob.order.cancelled.exchange";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "ob.order.cancelled";
    public static final String ORDER_CANCELLED_DLX = "ob.order.cancelled.warehouse.dlx";
    public static final String ORDER_CANCELLED_DLQ = "ob.order.cancelled.warehouse.dlq";

    // Inbound — shipping-service publishes, warehouse-service consumes
    public static final String SHIPMENT_DELIVERED_QUEUE = "ob.shipment.delivered.warehouse.queue";
    public static final String SHIPMENT_DELIVERED_EXCHANGE = "ob.shipment.delivered.exchange";
    public static final String SHIPMENT_DELIVERED_ROUTING_KEY = "ob.shipment.delivered";
    public static final String SHIPMENT_DELIVERED_DLX = "ob.shipment.delivered.warehouse.dlx";
    public static final String SHIPMENT_DELIVERED_DLQ = "ob.shipment.delivered.warehouse.dlq";

    // Outbound exchange only
    public static final String STOCK_RESERVED_EXCHANGE = "ob.stock.reserved.exchange";
    public static final String STOCK_RESERVED_ROUTING_KEY = "ob.stock.reserved";

    // Inbound — order placed
    @Bean
    public TopicExchange orderPlacedExchange() {
        return new TopicExchange(ORDER_PLACED_EXCHANGE);
    }

    @Bean
    public TopicExchange orderPlacedDlx() {
        return new TopicExchange(ORDER_PLACED_DLX);
    }

    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(ORDER_PLACED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_PLACED_DLX)
                .build();
    }

    @Bean
    public Queue orderPlacedDlq() {
        return QueueBuilder.durable(ORDER_PLACED_DLQ).build();
    }

    @Bean
    public Binding orderPlacedBinding(TopicExchange orderPlacedExchange,
                                      Queue orderPlacedQueue) {
        return BindingBuilder.bind(orderPlacedQueue)
                .to(orderPlacedExchange)
                .with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    public Binding orderPlacedDlqBinding(TopicExchange orderPlacedDlx,
                                         Queue orderPlacedDlq) {
        return BindingBuilder.bind(orderPlacedDlq)
                .to(orderPlacedDlx)
                .with("#");
    }

    // Inbound — order cancelled
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
                .withArgument("x-dead-letter-exchange", ORDER_CANCELLED_DLX)
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
                .withArgument("x-dead-letter-exchange", SHIPMENT_DELIVERED_DLX)
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

    // Outbound exchange only
    @Bean
    public TopicExchange stockReservedExchange() {
        return new TopicExchange(STOCK_RESERVED_EXCHANGE);
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}