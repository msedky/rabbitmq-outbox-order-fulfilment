package com.rabbitmqoutbox.shippingservice.model.enums;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}