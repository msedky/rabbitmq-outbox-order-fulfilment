package com.rabbitmqoutbox.shippingservice.exception;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(String message) {
        super(message);
    }
}