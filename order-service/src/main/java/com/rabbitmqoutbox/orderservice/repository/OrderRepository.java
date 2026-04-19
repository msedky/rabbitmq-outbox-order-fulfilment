package com.rabbitmqoutbox.orderservice.repository;

import com.rabbitmqoutbox.orderservice.model.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
}