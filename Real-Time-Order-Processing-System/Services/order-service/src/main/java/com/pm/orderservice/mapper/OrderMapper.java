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

    /**
     * Maps Order entity to OrderResponseDTO
     * Since both use 'orderItems' field name, no explicit @Mapping needed
     */
    OrderResponseDTO toResponseDTO(Order order);
    List<OrderResponseDTO> toResponseDTOList(List<Order> orders);

    // ==================== DTO TO ENTITY ====================

    /**
     * Maps OrderRequestDTO to Order entity
     * Field 'orderItems' automatically mapped (same name in DTO and entity)
     */
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Order toEntity(OrderRequestDTO dto);

    // ==================== UPDATE EXISTING ENTITY ====================

    /**
     * Updates existing Order from OrderUpdateDTO
     * Only non-null fields are updated
     */
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(OrderUpdateDTO dto, @MappingTarget Order order);
}
