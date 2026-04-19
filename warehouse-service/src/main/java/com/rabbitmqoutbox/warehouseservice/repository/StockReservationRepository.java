package com.rabbitmqoutbox.warehouseservice.repository;

import com.rabbitmqoutbox.warehouseservice.model.entity.StockReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, UUID> {
}