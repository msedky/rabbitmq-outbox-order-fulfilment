package com.rabbitmqoutbox.orderservice.exception.handler;

import com.rabbitmqoutbox.orderservice.exception.OrderNotFoundException;
import com.rabbitmqoutbox.orderservice.model.dto.response.ApiError;
import com.rabbitmqoutbox.orderservice.model.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFoundException(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .data(null)
                        .error(ApiError.builder()
                                .code("ORDER_NOT_FOUND")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .build())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
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
