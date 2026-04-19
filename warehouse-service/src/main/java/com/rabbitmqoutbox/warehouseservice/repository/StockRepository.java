package com.rabbitmqoutbox.warehouseservice.repository;

import com.rabbitmqoutbox.warehouseservice.model.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<StockEntity, UUID> {

    Optional<StockEntity> findByProductId(String productId);
}