package com.rabbitmqoutbox.orderservice.service.impl;

import com.rabbitmqoutbox.orderservice.exception.OrderNotFoundException;
import com.rabbitmqoutbox.orderservice.mapper.OrderMapper;
import com.rabbitmqoutbox.orderservice.messaging.event.OrderPlacedEvent;
import com.rabbitmqoutbox.orderservice.messaging.event.OrderPlacedEventItem;
import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderRequest;
import com.rabbitmqoutbox.orderservice.model.dto.response.OrderResponse;
import com.rabbitmqoutbox.orderservice.model.entity.OrderEntity;
import com.rabbitmqoutbox.orderservice.model.entity.OrderItemEntity;
import com.rabbitmqoutbox.orderservice.model.enums.OrderStatus;
import com.rabbitmqoutbox.orderservice.repository.OrderRepository;
import com.rabbitmqoutbox.orderservice.service.OrderService;
import com.rabbitmqoutbox.orderservice.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {

        List<OrderItemEntity> items = request.getItems().stream()
                .map(item -> OrderItemEntity.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEntity order = OrderEntity.builder()
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .deliveryAddress(request.getDeliveryAddress())
                .totalAmount(totalAmount)
                .currency(request.getCurrency())
                .status(OrderStatus.PENDING)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        OrderEntity savedOrder = orderRepository.save(order);

        log.info("Order saved with id={} and status=PENDING", savedOrder.getId());

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomerId())
                .customerEmail(savedOrder.getCustomerEmail())
                .deliveryAddress(savedOrder.getDeliveryAddress())
                .totalAmount(savedOrder.getTotalAmount())
                .currency(savedOrder.getCurrency())
                .items(items.stream()
                        .map(item -> OrderPlacedEventItem.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .toList())
                .occurredAt(Instant.now())
                .build();

        outboxEventService.saveEvent(
                savedOrder.getId().toString(),
                "ORDER",
                "ORDER_PLACED",
                event
        );

        log.info("Outbox event saved for orderId={}", savedOrder.getId());

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        return orderMapper.toResponseList(orderRepository.findAll());
    }
}