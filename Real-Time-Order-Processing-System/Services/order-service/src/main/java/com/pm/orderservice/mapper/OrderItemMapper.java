package com.pm.orderservice.mapper;

import com.pm.orderservice.dto.OrderItemRequestDTO;
import com.pm.orderservice.dto.OrderItemResponseDTO;
import com.pm.orderservice.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    // ==================== ENTITY TO DTO ====================
    OrderItemResponseDTO toResponseDTO(OrderItem orderItem);
    List<OrderItemResponseDTO> toResponseDTOList(List<OrderItem> orderItems);

    // ==================== DTO TO ENTITY ====================

    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "price", ignore = true)
    OrderItem toEntity(OrderItemRequestDTO dto);
    List<OrderItem> toEntityList(List<OrderItemRequestDTO> dtos);
}
