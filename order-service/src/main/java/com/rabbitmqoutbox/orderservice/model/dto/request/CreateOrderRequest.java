package com.rabbitmqoutbox.orderservice.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotBlank(message = "customerEmail is required")
    private String customerEmail;

    @NotBlank(message = "deliveryAddress is required")
    private String deliveryAddress;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotEmpty(message = "items are required")
    @Valid
    private List<CreateOrderItemRequest> items;
}