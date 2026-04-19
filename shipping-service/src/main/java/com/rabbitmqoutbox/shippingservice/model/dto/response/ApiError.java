package com.rabbitmqoutbox.shippingservice.model.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {

    private String code;
    private String message;
    private Instant timestamp;
}