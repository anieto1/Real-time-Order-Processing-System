package com.pm.orderservice.mapper;

import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.dto.OrderUpdateDTO;
import com.pm.orderservice.model.Order;
import org.mapstruct.*;
import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {OrderItemMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrderMapper {

    // ==================== ENTITY TO DTO ====================
    OrderResponseDTO toResponseDTO(Order order);
    List<OrderResponseDTO> toResponseDTOList(List<Order> orders);

    // ==================== DTO TO ENTITY ====================

    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(source = "items", target = "orderItems")
    Order toEntity(OrderRequestDTO dto);

    // ==================== UPDATE EXISTING ENTITY ====================

    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(source = "items", target = "orderItems")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(OrderUpdateDTO dto, @MappingTarget Order order);
}
