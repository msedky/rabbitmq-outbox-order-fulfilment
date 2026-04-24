package com.rabbitmqoutbox.shippingservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailShipmentRequest {

    @NotBlank(message = "failureReason is required")
    private String failureReason;
}