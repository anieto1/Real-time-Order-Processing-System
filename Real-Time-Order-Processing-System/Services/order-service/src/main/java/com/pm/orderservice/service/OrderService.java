package com.pm.orderservice.service;


import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.mapper.OrderMapper;
import com.pm.orderservice.model.Order;
import com.pm.orderservice.repository.OrderItemRepository;
import com.pm.orderservice.repository.OrderRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Builder
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;


    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
        if(orderRequestDTO.getOrderItems().isEmpty()){
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        Order newOrder = orderRepository.save(orderMapper.toEntity(orderRequestDTO));
        return orderMapper.toResponseDTO(newOrder);
    }



}
