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

    OrderResponseDTO toResponseDTO(Order order);
    List<OrderResponseDTO> toResponseDTOList(List<Order> orders);

    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Order toEntity(OrderRequestDTO dto);

    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(OrderUpdateDTO dto, @MappingTarget Order order);
}
