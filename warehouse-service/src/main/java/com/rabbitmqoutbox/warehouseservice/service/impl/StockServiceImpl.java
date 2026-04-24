package com.rabbitmqoutbox.warehouseservice.service.impl;

import com.rabbitmqoutbox.warehouseservice.exception.InsufficientStockException;
import com.rabbitmqoutbox.warehouseservice.exception.StockNotFoundException;
import com.rabbitmqoutbox.warehouseservice.mapper.StockMapper;
import com.rabbitmqoutbox.warehouseservice.messaging.event.*;
import com.rabbitmqoutbox.warehouseservice.model.dto.request.CreateStockRequest;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.StockResponse;
import com.rabbitmqoutbox.warehouseservice.model.entity.StockEntity;
import com.rabbitmqoutbox.warehouseservice.model.entity.StockReservationEntity;
import com.rabbitmqoutbox.warehouseservice.model.enums.ReservationStatus;
import com.rabbitmqoutbox.warehouseservice.repository.StockRepository;
import com.rabbitmqoutbox.warehouseservice.repository.StockReservationRepository;
import com.rabbitmqoutbox.warehouseservice.service.OutboxEventService;
import com.rabbitmqoutbox.warehouseservice.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;
    private final OutboxEventService outboxEventService;
    private final StockMapper stockMapper;

    @Override
    @Transactional
    public StockResponse create(CreateStockRequest request) {
        StockEntity stock = StockEntity.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .availableQuantity(request.getAvailableQuantity())
                .reservedQuantity(0)
                .build();

        StockEntity savedStock = stockRepository.saveAndFlush(stock);
        log.info("Stock created for productId={}", savedStock.getProductId());
        return stockMapper.toResponse(savedStock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> getAll() {
        return stockMapper.toResponseList(stockRepository.findAll());
    }

    @Override
    @Transactional
    public void reserveStock(OrderPlacedEvent event) {
        for (OrderPlacedEventItem item : event.getItems()) {

            StockEntity stock = stockRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(
                            "Stock not found for productId: " + item.getProductId()));

            if (stock.getAvailableQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for productId: " + item.getProductId());
            }

            int quantityBefore = stock.getAvailableQuantity();

            stock.setAvailableQuantity(stock.getAvailableQuantity() - item.getQuantity());
            stock.setReservedQuantity(stock.getReservedQuantity() + item.getQuantity());
            stockRepository.save(stock);

            StockReservationEntity reservation = StockReservationEntity.builder()
                    .orderId(event.getOrderId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .quantityBeforeReservation(quantityBefore)
                    .quantityAfterReservation(stock.getAvailableQuantity())
                    .build();

            stockReservationRepository.save(reservation);

            log.info("Stock reserved for productId={}, orderId={}",
                    item.getProductId(), event.getOrderId());
        }

        StockReservedEvent stockReservedEvent = StockReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .customerEmail(event.getCustomerEmail())
                .deliveryAddress(event.getDeliveryAddress())
                .items(event.getItems().stream()
                        .map(item -> StockReservedEventItem.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .occurredAt(Instant.now())
                .build();

        outboxEventService.saveEvent(
                event.getOrderId().toString(),
                "STOCK",
                "STOCK_RESERVED",
                stockReservedEvent
        );

        log.info("Outbox event saved for STOCK_RESERVED, orderId={}", event.getOrderId());
    }

    @Override
    @Transactional
    public void releaseStock(OrderCancelledEvent event) {

        List<StockReservationEntity> reservations = stockReservationRepository
                .findByOrderIdAndStatus(event.getOrderId(), ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            log.warn("No RESERVED reservations found for orderId={}", event.getOrderId());
            return;
        }

        for (StockReservationEntity reservation : reservations) {

            StockEntity stock = stockRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(
                            "Stock not found for productId: " + reservation.getProductId()));

            stock.setAvailableQuantity(
                    stock.getAvailableQuantity() + reservation.getQuantity());
            stock.setReservedQuantity(
                    stock.getReservedQuantity() - reservation.getQuantity());
            stockRepository.save(stock);

            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReleasedAt(Instant.now());
            stockReservationRepository.save(reservation);

            log.info("Stock released for productId={}, orderId={}",
                    reservation.getProductId(), event.getOrderId());
        }
    }

    @Override
    @Transactional
    public void fulfillStock(ShipmentOutForDeliveryEvent event) {

        List<StockReservationEntity> reservations = stockReservationRepository
                .findByOrderIdAndStatus(event.getOrderId(), ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            log.warn("No RESERVED reservations found for orderId={}", event.getOrderId());
            return;
        }

        for (StockReservationEntity reservation : reservations) {

            StockEntity stock = stockRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new StockNotFoundException(
                            "Stock not found for productId: " + reservation.getProductId()));

            stock.setReservedQuantity(stock.getReservedQuantity() - reservation.getQuantity());
            stockRepository.save(stock);

            reservation.setStatus(ReservationStatus.FULFILLED);
            reservation.setFulfilledAt(Instant.now());
            stockReservationRepository.save(reservation);

            log.info("Stock fulfilled for productId={}, orderId={}",
                    reservation.getProductId(), event.getOrderId());
        }
    }
}