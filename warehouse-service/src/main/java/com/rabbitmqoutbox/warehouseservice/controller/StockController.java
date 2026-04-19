package com.rabbitmqoutbox.warehouseservice.controller;

import com.rabbitmqoutbox.warehouseservice.model.dto.request.CreateStockRequest;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.ApiResponse;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.StockResponse;
import com.rabbitmqoutbox.warehouseservice.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StockResponse> createStock(
            @Valid @RequestBody CreateStockRequest request) {
        return ApiResponse.<StockResponse>builder()
                .success(true)
                .data(stockService.create(request))
                .error(null)
                .build();
    }

    @GetMapping
    public ApiResponse<List<StockResponse>> getAllStocks() {
        return ApiResponse.<List<StockResponse>>builder()
                .success(true)
                .data(stockService.getAll())
                .error(null)
                .build();
    }
}