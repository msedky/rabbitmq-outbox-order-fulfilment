package com.rabbitmqoutbox.shippingservice.controller;

import com.rabbitmqoutbox.shippingservice.model.dto.response.ApiResponse;
import com.rabbitmqoutbox.shippingservice.model.dto.response.ShipmentResponse;
import com.rabbitmqoutbox.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/{shipmentId}")
    public ApiResponse<ShipmentResponse> getShipmentById(@PathVariable UUID shipmentId) {
        return ApiResponse.<ShipmentResponse>builder()
                .success(true)
                .data(shipmentService.getById(shipmentId))
                .error(null)
                .build();
    }

    @GetMapping
    public ApiResponse<List<ShipmentResponse>> getAllShipments() {
        return ApiResponse.<List<ShipmentResponse>>builder()
                .success(true)
                .data(shipmentService.getAll())
                .error(null)
                .build();
    }
}