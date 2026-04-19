package com.rabbitmqoutbox.orderservice.model.enums;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}