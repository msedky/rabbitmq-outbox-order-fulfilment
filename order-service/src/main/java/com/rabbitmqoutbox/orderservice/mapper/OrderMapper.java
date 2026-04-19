package com.rabbitmqoutbox.orderservice.mapper;

import com.rabbitmqoutbox.orderservice.model.dto.response.OrderItemResponse;
import com.rabbitmqoutbox.orderservice.model.dto.response.OrderResponse;
import com.rabbitmqoutbox.orderservice.model.entity.OrderEntity;
import com.rabbitmqoutbox.orderservice.model.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "status", target = "status")
    OrderResponse toResponse(OrderEntity order);

    @Mapping(source = "id", target = "itemId")
    OrderItemResponse toItemResponse(OrderItemEntity item);

    List<OrderResponse> toResponseList(List<OrderEntity> orders);
}