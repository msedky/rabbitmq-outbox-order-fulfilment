package com.rabbitmqoutbox.orderservice.service.impl;

import com.rabbitmqoutbox.orderservice.exception.OrderNotFoundException;
import com.rabbitmqoutbox.orderservice.mapper.OrderMapper;
import com.rabbitmqoutbox.orderservice.messaging.event.*;
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

        OrderEntity savedOrder = orderRepository.saveAndFlush(order);

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
    @Transactional
    public OrderResponse cancel(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a delivered order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException(
                    "Cannot cancel an order that is already out for delivery");
        }

        if (order.getStatus() == OrderStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a failed order");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        OrderEntity savedOrder = orderRepository.save(order);

        log.info("Order cancelled with id={}", orderId);

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomerId())
                .customerEmail(savedOrder.getCustomerEmail())
                .occurredAt(Instant.now())
                .build();

        outboxEventService.saveEvent(
                savedOrder.getId().toString(),
                "ORDER",
                "ORDER_CANCELLED",
                event
        );

        log.info("Outbox event saved for ORDER_CANCELLED, orderId={}", orderId);

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

    @Override
    @Transactional
    public void confirmOrder(StockReservedEvent event) {
        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + event.getOrderId()));

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        orderRepository.save(order);

        log.info("Order confirmed for orderId={}", event.getOrderId());
    }

    @Override
    @Transactional
    public void markAsShipped(ShipmentScheduledEvent event) {
        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + event.getOrderId()));

        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(Instant.now());
        orderRepository.save(order);

        log.info("Order marked as SHIPPED for orderId={}", event.getOrderId());
    }

    @Override
    @Transactional
    public void markAsDelivered(ShipmentDeliveredEvent event) {
        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + event.getOrderId()));

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(event.getDeliveredAt());
        orderRepository.save(order);

        log.info("Order marked as DELIVERED for orderId={}", event.getOrderId());
    }

    @Override
    @Transactional
    public void markAsOutForDelivery(ShipmentOutForDeliveryEvent event) {
        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + event.getOrderId()));

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDispatchedAt(event.getDispatchedAt());
        orderRepository.save(order);

        log.info("Order marked as OUT_FOR_DELIVERY for orderId={}", event.getOrderId());
    }

    @Override
    @Transactional
    public void markAsFailed(ShipmentFailedEvent event) {
        OrderEntity order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + event.getOrderId()));

        order.setStatus(OrderStatus.FAILED);
        order.setFailedAt(event.getFailedAt());
        orderRepository.save(order);

        log.info("Order marked as FAILED for orderId={}", event.getOrderId());
    }
}