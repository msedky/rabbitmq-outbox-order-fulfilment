package com.rabbitmqoutbox.warehouseservice.repository;

import com.rabbitmqoutbox.warehouseservice.model.entity.StockReservationEntity;
import com.rabbitmqoutbox.warehouseservice.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, UUID> {

    List<StockReservationEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}