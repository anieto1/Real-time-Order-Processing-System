package com.pm.orderservice.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
        if (orderRequestDTO.getOrderItems() == null || orderRequestDTO.getOrderItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        Order order = orderMapper.toEntity(orderRequestDTO);

        for (OrderItem item : order.getOrderItems()) {
            BigDecimal price = fetchProductPrice(item.getProductId());
            item.setPrice(price);
            item.setOrder(order);
        }

        BigDecimal totalAmount = calculateTotalAmount(order);
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(savedOrder.getOrderId())
                .aggregateType("ORDER")
                .eventType(EventType.ORDER_CREATED)
                .payload("{\"orderId\":\"123\", \"customerId\":\"456\"}")
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

    private BigDecimal fetchProductPrice(UUID productId) {
        return BigDecimal.valueOf(99.99);
    }
}
