package com.rabbitmqoutbox.warehouseservice.repository;

import com.rabbitmqoutbox.warehouseservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.warehouseservice.model.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByStatus(OutboxStatus status);
}