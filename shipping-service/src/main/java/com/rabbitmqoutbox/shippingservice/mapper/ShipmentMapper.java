package com.rabbitmqoutbox.shippingservice.mapper;

import com.rabbitmqoutbox.shippingservice.model.dto.response.ShipmentResponse;
import com.rabbitmqoutbox.shippingservice.model.entity.ShipmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface ShipmentMapper {

    @Mapping(source = "id", target = "shipmentId")
    @Mapping(source = "status", target = "status")
    ShipmentResponse toResponse(ShipmentEntity shipment);

    List<ShipmentResponse> toResponseList(List<ShipmentEntity> shipments);
}