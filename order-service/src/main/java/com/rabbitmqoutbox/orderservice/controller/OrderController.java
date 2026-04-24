package com.rabbitmqoutbox.orderservice.controller;

import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderRequest;
import com.rabbitmqoutbox.orderservice.model.dto.response.ApiResponse;
import com.rabbitmqoutbox.orderservice.model.dto.response.OrderResponse;
import com.rabbitmqoutbox.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .data(orderService.create(request))
                .error(null)
                .build();
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .data(orderService.cancel(orderId))
                .error(null)
                .build();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .data(orderService.getById(orderId))
                .error(null)
                .build();
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .success(true)
                .data(orderService.getAll())
                .error(null)
                .build();
    }
}