package com.rabbitmqoutbox.orderservice.service;

import com.rabbitmqoutbox.orderservice.messaging.event.*;
import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderRequest;
import com.rabbitmqoutbox.orderservice.model.dto.response.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse create(CreateOrderRequest request);

    OrderResponse cancel(UUID orderId);

    OrderResponse getById(UUID orderId);

    List<OrderResponse> getAll();

    void confirmOrder(StockReservedEvent event);

    void markAsShipped(ShipmentScheduledEvent event);

    void markAsDelivered(ShipmentDeliveredEvent event);

    void markAsOutForDelivery(ShipmentOutForDeliveryEvent event);

    void markAsFailed(ShipmentFailedEvent event);
}