package com.rabbitmqoutbox.warehouseservice.service;

import com.rabbitmqoutbox.warehouseservice.messaging.event.OrderCancelledEvent;
import com.rabbitmqoutbox.warehouseservice.messaging.event.OrderPlacedEvent;
import com.rabbitmqoutbox.warehouseservice.messaging.event.ShipmentOutForDeliveryEvent;
import com.rabbitmqoutbox.warehouseservice.model.dto.request.CreateStockRequest;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.StockResponse;

import java.util.List;

public interface StockService {

    StockResponse create(CreateStockRequest request);

    List<StockResponse> getAll();

    void reserveStock(OrderPlacedEvent event);

    void releaseStock(OrderCancelledEvent event);

    void fulfillStock(ShipmentOutForDeliveryEvent event);
}