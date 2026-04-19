package com.rabbitmqoutbox.shippingservice.repository;

import com.rabbitmqoutbox.shippingservice.model.entity.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID> {
}