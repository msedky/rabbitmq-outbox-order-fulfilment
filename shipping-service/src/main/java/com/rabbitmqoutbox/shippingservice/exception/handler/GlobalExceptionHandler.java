package com.rabbitmqoutbox.shippingservice.exception.handler;

import com.rabbitmqoutbox.shippingservice.exception.ShipmentNotFoundException;
import com.rabbitmqoutbox.shippingservice.model.dto.response.ApiError;
import com.rabbitmqoutbox.shippingservice.model.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleShipmentNotFoundException(
            ShipmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .data(null)
                        .error(ApiError.builder()
                                .code("SHIPMENT_NOT_FOUND")
                                .message(ex.getMessage())
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