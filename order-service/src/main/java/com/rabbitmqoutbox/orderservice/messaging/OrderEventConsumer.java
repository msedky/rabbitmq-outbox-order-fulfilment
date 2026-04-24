package com.rabbitmqoutbox.orderservice.messaging;

import com.rabbitmqoutbox.orderservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.orderservice.exception.OrderNotFoundException;
import com.rabbitmqoutbox.orderservice.messaging.event.*;
import com.rabbitmqoutbox.orderservice.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVED_QUEUE)
    public void handleStockReserved(StockReservedEvent event,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                    @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        try {
            orderService.confirmOrder(event);
            channel.basicAck(deliveryTag, false);
        } catch (OrderNotFoundException e) {
            log.error("Non-retryable failure for orderId={}: {}",
                    event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= 3) {
                log.error("Max retries reached for orderId={}, sending to DLQ",
                        event.getOrderId());
                channel.basicReject(deliveryTag, false);
            } else {
                log.warn("Retryable failure attempt {} for orderId={}",
                        retryCount + 1, event.getOrderId());
                channel.basicReject(deliveryTag, true);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_SCHEDULED_QUEUE)
    public void handleShipmentScheduled(ShipmentScheduledEvent event,
                                        Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                        @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        try {
            orderService.markAsShipped(event);
            channel.basicAck(deliveryTag, false);
        } catch (OrderNotFoundException e) {
            log.error("Non-retryable failure for orderId={}: {}",
                    event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= 3) {
                log.error("Max retries reached for orderId={}, sending to DLQ",
                        event.getOrderId());
                channel.basicReject(deliveryTag, false);
            } else {
                log.warn("Retryable failure attempt {} for orderId={}",
                        retryCount + 1, event.getOrderId());
                channel.basicReject(deliveryTag, true);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_DELIVERED_QUEUE)
    public void handleShipmentDelivered(ShipmentDeliveredEvent event,
                                        Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                        @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        try {
            orderService.markAsDelivered(event);
            channel.basicAck(deliveryTag, false);
        } catch (OrderNotFoundException e) {
            log.error("Non-retryable failure for orderId={}: {}",
                    event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= 3) {
                log.error("Max retries reached for orderId={}, sending to DLQ",
                        event.getOrderId());
                channel.basicReject(deliveryTag, false);
            } else {
                log.warn("Retryable failure attempt {} for orderId={}",
                        retryCount + 1, event.getOrderId());
                channel.basicReject(deliveryTag, true);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_OUT_FOR_DELIVERY_QUEUE)
    public void handleShipmentOutForDelivery(ShipmentOutForDeliveryEvent event,
                                             Channel channel,
                                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                             @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        try {
            orderService.markAsOutForDelivery(event);
            channel.basicAck(deliveryTag, false);
        } catch (OrderNotFoundException e) {
            log.error("Non-retryable failure for orderId={}: {}",
                    event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= 3) {
                log.error("Max retries reached for orderId={}, sending to DLQ",
                        event.getOrderId());
                channel.basicReject(deliveryTag, false);
            } else {
                log.warn("Retryable failure attempt {} for orderId={}",
                        retryCount + 1, event.getOrderId());
                channel.basicReject(deliveryTag, true);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_FAILED_QUEUE)
    public void handleShipmentFailed(ShipmentFailedEvent event,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                     @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        try {
            orderService.markAsFailed(event);
            channel.basicAck(deliveryTag, false);
        } catch (OrderNotFoundException e) {
            log.error("Non-retryable failure for orderId={}: {}",
                    event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= 3) {
                log.error("Max retries reached for orderId={}, sending to DLQ",
                        event.getOrderId());
                channel.basicReject(deliveryTag, false);
            } else {
                log.warn("Retryable failure attempt {} for orderId={}",
                        retryCount + 1, event.getOrderId());
                channel.basicReject(deliveryTag, true);
            }
        }
    }

    private long getRetryCount(List<Map<String, Object>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) return 0L;
        Object count = xDeath.get(0).get("count");
        return count instanceof Long ? (Long) count : 0L;
    }
}