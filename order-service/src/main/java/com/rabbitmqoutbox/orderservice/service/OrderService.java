package com.rabbitmqoutbox.orderservice.service;

import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderRequest;
import com.rabbitmqoutbox.orderservice.model.dto.response.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse create(CreateOrderRequest request);

    OrderResponse getById(UUID orderId);

    List<OrderResponse> getAll();
}