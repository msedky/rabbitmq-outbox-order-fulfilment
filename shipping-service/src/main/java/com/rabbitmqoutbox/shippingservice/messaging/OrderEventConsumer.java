package com.rabbitmqoutbox.shippingservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqoutbox.shippingservice.config.RabbitMQConfig;
import com.rabbitmqoutbox.shippingservice.exception.ShipmentNotFoundException;
import com.rabbitmqoutbox.shippingservice.messaging.event.OrderCancelledEvent;
import com.rabbitmqoutbox.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ShipmentService shipmentService;

    @Value("${spring.rabbitmq.listener.simple.retry.max-retries}")
    private int maxRetries;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                     @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath)
            throws IOException {
        try {
            shipmentService.cancelShipment(event.getOrderId());
            channel.basicAck(deliveryTag, false);
        } catch (IllegalStateException | ShipmentNotFoundException e) {
            log.error("Non-retryable failure for orderId={}: {}",
                    event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= maxRetries) {
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