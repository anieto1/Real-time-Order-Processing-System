package com.pm.orderservice.service;


import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.model.Order;
import com.pm.orderservice.repository.OrderItemRepository;
import com.pm.orderservice.repository.OrderRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import lombok.Builder;
import org.springframework.stereotype.Service;

@Builder
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventRepository outboxEventRepository;

//    public OrderRequestDTO createOrder(OrderRequestDTO orderRequestDTO) {
//        Order newOrder = orderRepository.save(Order.builder()
//                .customerId(orderRequestDTO.getCustomerId())
//                .build());
//
//    }



}
