package com.rabbitmqoutbox.warehouseservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqoutbox.warehouseservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.warehouseservice.exception.InsufficientStockException;
import com.rabbitmqoutbox.warehouseservice.exception.StockNotFoundException;
import com.rabbitmqoutbox.warehouseservice.messaging.event.OrderCancelledEvent;
import com.rabbitmqoutbox.warehouseservice.messaging.event.OrderPlacedEvent;
import com.rabbitmqoutbox.warehouseservice.messaging.event.ShipmentOutForDeliveryEvent;
import com.rabbitmqoutbox.warehouseservice.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String NON_RETRYABLE_FAILURE_LOG = "Non-retryable failure for orderId={}: {}";
    private static final String MAX_RETRIES_REACHED_LOG = "Max retries reached for orderId={}, sending to DLQ";
    private static final String RETRYABLE_FAILURE_LOG = "Retryable failure attempt {} for orderId={}";

    private final StockService stockService;

    @Value("${spring.rabbitmq.listener.simple.retry.max-retries}")
    private int maxRetries;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        handleWithRetry(event.getOrderId(), channel, deliveryTag, xDeath,
                () -> stockService.reserveStock(event));
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                     @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        handleWithRetry(event.getOrderId(), channel, deliveryTag, xDeath,
                () -> stockService.releaseStock(event));
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_OUT_FOR_DELIVERY_QUEUE)
    public void handleShipmentOutForDelivery(ShipmentOutForDeliveryEvent event,
                                             Channel channel,
                                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                             @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        handleWithRetry(event.getOrderId(), channel, deliveryTag, xDeath,
                () -> stockService.fulfillStock(event));
    }

    private void handleWithRetry(UUID orderId,
                                 Channel channel,
                                 long deliveryTag,
                                 List<Map<String, Object>> xDeath,
                                 MessageHandler handler) throws IOException {
        try {
            handler.handle();
            channel.basicAck(deliveryTag, false);
        } catch (StockNotFoundException | InsufficientStockException e) {
            log.error(NON_RETRYABLE_FAILURE_LOG, orderId, e.getMessage());
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= maxRetries) {
                log.error(MAX_RETRIES_REACHED_LOG, orderId);
                channel.basicReject(deliveryTag, false);
            } else {
                log.warn(RETRYABLE_FAILURE_LOG, retryCount + 1, orderId);
                channel.basicReject(deliveryTag, true);
            }
        }
    }

    @FunctionalInterface
    private interface MessageHandler {
        void handle() throws MessageHandlingException;
    }

    private long getRetryCount(List<Map<String, Object>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) return 0L;
        Object count = xDeath.get(0).get("count");
        return count instanceof Long l ? l : 0L;
    }
}