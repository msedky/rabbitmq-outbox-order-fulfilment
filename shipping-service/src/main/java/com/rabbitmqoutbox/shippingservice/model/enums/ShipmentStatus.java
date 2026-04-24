package com.rabbitmqoutbox.shippingservice.model.enums;

public enum ShipmentStatus {
    SCHEDULED,        // shipment created, waiting for courier pickup
    OUT_FOR_DELIVERY, // with local courier, will be delivered today
    DELIVERED,        // successfully handed to customer
    FAILED            // delivery attempt failed
}