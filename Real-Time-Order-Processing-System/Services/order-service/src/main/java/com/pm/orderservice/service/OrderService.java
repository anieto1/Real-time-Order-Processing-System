package com.pm.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.dto.OrderUpdateDTO;
import com.pm.orderservice.exception.InvalidOrderException;
import com.pm.orderservice.exception.InvalidOrderStateException;
import com.pm.orderservice.exception.OrderNotFoundException;
import com.pm.orderservice.mapper.OrderMapper;
import com.pm.orderservice.model.*;
import com.pm.orderservice.repository.OrderItemRepository;
import com.pm.orderservice.repository.OrderRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
        if (orderRequestDTO.getOrderItems() == null || orderRequestDTO.getOrderItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        Order order = orderMapper.toEntity(orderRequestDTO);

        for (OrderItem item : order.getOrderItems()) {
            item.setOrder(order);
        }

        BigDecimal totalAmount = calculateTotalAmount(order);
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Create outbox event with actual order data
        String payload;
        try {
            OrderResponseDTO orderResponse = orderMapper.toResponseDTO(savedOrder);
            payload = objectMapper.writeValueAsString(orderResponse);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order to JSON for orderId: {}", savedOrder.getOrderId(), e);
            throw new RuntimeException("Failed to create order event", e);
        }

        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(savedOrder.getOrderId())
                .aggregateType("ORDER")
                .eventType(EventType.ORDER_CREATED)
                .payload(payload)
                .published(false)
                .build();
        outboxEventRepository.save(event);

        return orderMapper.toResponseDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return orderPage.map(orderMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public OrderResponseDTO updateOrder(UUID orderId, OrderUpdateDTO orderUpdateDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getOrderStatus() == Status.COMPLETED || order.getOrderStatus() == Status.CANCELLED) {
            throw new InvalidOrderStateException(orderId, order.getOrderStatus(), "update order");
        }

        orderMapper.updateEntityFromDTO(orderUpdateDTO, order);

        if (orderUpdateDTO.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getPrice() == null) {
                    BigDecimal price = fetchProductPrice(item.getProductId());
                    item.setPrice(price);
                }
                item.setOrder(order);
            }

            BigDecimal totalAmount = calculateTotalAmount(order);
            order.setTotalAmount(totalAmount);
        }

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getOrderStatus() == Status.COMPLETED) {
            throw new InvalidOrderStateException(orderId, order.getOrderStatus(), "cancel completed order");
        }

        if (order.getOrderStatus() == Status.CANCELLED) {
            throw new InvalidOrderStateException(orderId, order.getOrderStatus(), "cancel already cancelled order");
        }

        order.setOrderStatus(Status.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO completeOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getOrderStatus() == Status.CANCELLED) {
            throw new InvalidOrderStateException(orderId, order.getOrderStatus(), "complete cancelled order");
        }

        if (order.getOrderStatus() == Status.COMPLETED) {
            throw new InvalidOrderStateException(orderId, order.getOrderStatus(), "complete already completed order");
        }

        order.setOrderStatus(Status.COMPLETED);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(savedOrder);
    }

    private BigDecimal calculateTotalAmount(Order order) {
        return order.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

     //TODO: Implement this method when Product Service is integrated
     //This will fetch real-time product prices and validate product IDs
     private BigDecimal fetchProductPrice(UUID productId) {
         // Will call Product Service via REST or gRPC
         throw new UnsupportedOperationException("Product Service integration pending");
     }
}
