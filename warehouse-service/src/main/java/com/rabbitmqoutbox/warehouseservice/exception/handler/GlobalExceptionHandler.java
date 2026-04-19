package com.rabbitmqoutbox.warehouseservice.exception.handler;

import com.rabbitmqoutbox.warehouseservice.exception.InsufficientStockException;
import com.rabbitmqoutbox.warehouseservice.exception.StockNotFoundException;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.ApiError;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockNotFoundException(
            StockNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .data(null)
                        .error(ApiError.builder()
                                .code("STOCK_NOT_FOUND")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .build())
                        .build());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStockException(
            InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .data(null)
                        .error(ApiError.builder()
                                .code("INSUFFICIENT_STOCK")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .build())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .data(null)
                        .error(ApiError.builder()
                                .code("VALIDATION_ERROR")
                                .message(message)
                                .timestamp(Instant.now())
                                .build())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .data(null)
                        .error(ApiError.builder()
                                .code("INTERNAL_SERVER_ERROR")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .build())
                        .build());
    }
}