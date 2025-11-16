package com.pm.orderservice.mapper;

import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.model.Order;
import org.mapstruct.Mapper;

@Mapper
public class OrderMapper {
    public static OrderResponseDTO toOrderResponseDTO(Order order){
        if(order == null) return null;



    }
}
