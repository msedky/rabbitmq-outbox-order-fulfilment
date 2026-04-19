package com.rabbitmqoutbox.shippingservice.service;

import com.rabbitmqoutbox.shippingservice.messaging.event.StockReservedEvent;
import com.rabbitmqoutbox.shippingservice.model.dto.response.ShipmentResponse;

import java.util.List;
import java.util.UUID;

public interface ShipmentService {

    void scheduleShipment(StockReservedEvent event);

    ShipmentResponse getById(UUID shipmentId);

    List<ShipmentResponse> getAll();
}