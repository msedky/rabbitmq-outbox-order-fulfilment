package com.rabbitmqoutbox.orderservice.model.enums;

public enum OrderStatus {
    PENDING,          // order created
    CONFIRMED,        // stock reserved
    SHIPPED,          // shipment scheduled
    OUT_FOR_DELIVERY, // courier picked up package
    DELIVERED,        // handed to customer
    CANCELLED,        // cancelled by customer
    FAILED            // delivery failed
}