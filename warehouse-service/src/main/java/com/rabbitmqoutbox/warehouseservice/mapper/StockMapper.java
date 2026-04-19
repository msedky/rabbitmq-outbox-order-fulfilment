package com.rabbitmqoutbox.warehouseservice.mapper;

import com.rabbitmqoutbox.warehouseservice.model.dto.response.StockResponse;
import com.rabbitmqoutbox.warehouseservice.model.entity.StockEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface StockMapper {

    @Mapping(source = "id", target = "stockId")
    StockResponse toResponse(StockEntity stock);

    List<StockResponse> toResponseList(List<StockEntity> stocks);
}