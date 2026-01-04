package com.pm.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.exception.InvalidOrderException;
import com.pm.orderservice.exception.InvalidOrderStateException;
import com.pm.orderservice.exception.OrderNotFoundException;
import com.pm.orderservice.mapper.OrderMapper;
import com.pm.orderservice.model.Order;
import com.pm.orderservice.model.OutboxEvent;
import com.pm.orderservice.model.Status;
import com.pm.orderservice.repository.OrderItemRepository;
import com.pm.orderservice.repository.OrderRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import com.pm.orderservice.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    // ========================================
    // CREATE ORDER TESTS
    // ========================================

    @Nested
    @DisplayName("createOrder Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should successfully create order with valid data")
        void createOrder_WithValidData_ShouldSucceed() throws JsonProcessingException {
            // Arrange
            OrderRequestDTO requestDTO = TestDataBuilder.buildValidOrderRequest();
            Order mockOrder = TestDataBuilder.buildOrder();
            OrderResponseDTO responseDTO = TestDataBuilder.buildOrderResponseDTO();

            when(orderMapper.toEntity(any(OrderRequestDTO.class))).thenReturn(mockOrder);
            when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
            when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":\"test\"}");

            // Act
            OrderResponseDTO result = orderService.createOrder(requestDTO);

            // Assert
            assertNotNull(result);
            verify(orderMapper, times(1)).toEntity(requestDTO);
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
            verify(orderMapper, times(2)).toResponseDTO(mockOrder); // Called twice: for payload and return
        }

        @Test
        @DisplayName("Should throw exception when order has empty items")
        void createOrder_WithEmptyItems_ShouldThrowException() {
            // Arrange
            OrderRequestDTO requestDTO = TestDataBuilder.buildOrderRequestWithEmptyItems();

            // Act & Assert
            InvalidOrderException exception = assertThrows(
                    InvalidOrderException.class,
                    () -> orderService.createOrder(requestDTO)
            );

            assertTrue(exception.getMessage().contains("at least one item"));
            verify(orderRepository, never()).save(any(Order.class));
            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when order has null items")
        void createOrder_WithNullItems_ShouldThrowException() {
            // Arrange
            OrderRequestDTO requestDTO = TestDataBuilder.buildOrderRequestWithNullItems();

            // Act & Assert
            InvalidOrderException exception = assertThrows(
                    InvalidOrderException.class,
                    () -> orderService.createOrder(requestDTO)
            );

            assertTrue(exception.getMessage().contains("at least one item"));
            verify(orderRepository, never()).save(any(Order.class));
            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when JSON serialization fails")
        void createOrder_WhenJsonSerializationFails_ShouldThrowRuntimeException() throws JsonProcessingException {
            // Arrange
            OrderRequestDTO requestDTO = TestDataBuilder.buildValidOrderRequest();
            Order mockOrder = TestDataBuilder.buildOrder();
            OrderResponseDTO responseDTO = TestDataBuilder.buildOrderResponseDTO();

            when(orderMapper.toEntity(any(OrderRequestDTO.class))).thenReturn(mockOrder);
            when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
            when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);
            when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> orderService.createOrder(requestDTO)
            );

            assertTrue(exception.getMessage().contains("Failed to create order event"));
            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }
    }

    // ========================================
    // GET ORDER BY ID TESTS
    // ========================================

    @Nested
    @DisplayName("getOrderById Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order when found")
        void getOrderById_WhenExists_ShouldReturnOrder() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order mockOrder = TestDataBuilder.buildOrder();
            mockOrder.setOrderId(orderId);
            OrderResponseDTO responseDTO = TestDataBuilder.buildOrderResponseDTO();
            responseDTO.setOrderId(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
            when(orderMapper.toResponseDTO(mockOrder)).thenReturn(responseDTO);

            // Act
            OrderResponseDTO result = orderService.getOrderById(orderId);

            // Assert
            assertNotNull(result);
            assertEquals(orderId, result.getOrderId());
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderMapper, times(1)).toResponseDTO(mockOrder);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void getOrderById_WhenNotExists_ShouldThrowException() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            OrderNotFoundException exception = assertThrows(
                    OrderNotFoundException.class,
                    () -> orderService.getOrderById(orderId)
            );

            assertNotNull(exception);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderMapper, never()).toResponseDTO(any());
        }
    }

    // ========================================
    // GET ALL ORDERS TESTS
    // ========================================

    @Nested
    @DisplayName("getAllOrders Tests")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should return paginated orders")
        void getAllOrders_ShouldReturnPaginatedResults() {
            // Arrange
            Order mockOrder = TestDataBuilder.buildOrder();
            OrderResponseDTO responseDTO = TestDataBuilder.buildOrderResponseDTO();
            Page<Order> orderPage = new PageImpl<>(List.of(mockOrder));

            when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);
            when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);

            // Act
            Page<OrderResponseDTO> result = orderService.getAllOrders(0, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(orderRepository, times(1)).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no orders exist")
        void getAllOrders_WhenEmpty_ShouldReturnEmptyPage() {
            // Arrange
            Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // Act
            Page<OrderResponseDTO> result = orderService.getAllOrders(0, 10);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }
    }

    // ========================================
    // GET ORDERS BY CUSTOMER ID TESTS
    // ========================================

    @Nested
    @DisplayName("getOrdersByCustomerId Tests")
    class GetOrdersByCustomerIdTests {

        @Test
        @DisplayName("Should return orders for existing customer")
        void getOrdersByCustomerId_WhenOrdersExist_ShouldReturnList() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            Order mockOrder = TestDataBuilder.buildOrderForCustomer(customerId);
            OrderResponseDTO responseDTO = TestDataBuilder.buildOrderResponseDTO();
            responseDTO.setCustomerId(customerId);

            when(orderRepository.findByCustomerId(customerId)).thenReturn(List.of(mockOrder));
            when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);

            // Act
            List<OrderResponseDTO> result = orderService.getOrdersByCustomerId(customerId);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(orderRepository, times(1)).findByCustomerId(customerId);
        }

        @Test
        @DisplayName("Should return empty list when customer has no orders")
        void getOrdersByCustomerId_WhenNoOrders_ShouldReturnEmptyList() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            when(orderRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

            // Act
            List<OrderResponseDTO> result = orderService.getOrdersByCustomerId(customerId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(orderRepository, times(1)).findByCustomerId(customerId);
        }
    }

    // ========================================
    // CANCEL ORDER TESTS
    // ========================================

    @Nested
    @DisplayName("cancelOrder Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should successfully cancel pending order")
        void cancelOrder_WhenPending_ShouldSucceed() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order mockOrder = TestDataBuilder.buildOrderWithStatus(Status.PENDING);
            mockOrder.setOrderId(orderId);

            Order cancelledOrder = TestDataBuilder.buildOrderWithStatus(Status.CANCELLED);
            cancelledOrder.setOrderId(orderId);

            OrderResponseDTO responseDTO = TestDataBuilder.buildOrderResponseDTO();
            responseDTO.setOrderId(orderId);
            responseDTO.setOrderStatus(Status.CANCELLED);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);
            when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);

            // Act
            OrderResponseDTO result = orderService.cancelOrder(orderId);

            // Assert
            assertNotNull(result);
            assertEquals(Status.CANCELLED, result.getOrderStatus());
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when cancelling completed order")
        void cancelOrder_WhenCompleted_ShouldThrowException() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order completedOrder = TestDataBuilder.buildOrderWithStatus(Status.COMPLETED);
            completedOrder.setOrderId(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

            // Act & Assert
            InvalidOrderStateException exception = assertThrows(
                    InvalidOrderStateException.class,
                    () -> orderService.cancelOrder(orderId)
            );

            assertNotNull(exception);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when cancelling already cancelled order")
        void cancelOrder_WhenAlreadyCancelled_ShouldThrowException() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order cancelledOrder = TestDataBuilder.buildOrderWithStatus(Status.CANCELLED);
            cancelledOrder.setOrderId(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(cancelledOrder));

            // Act & Assert
            InvalidOrderStateException exception = assertThrows(
                    InvalidOrderStateException.class,
                    () -> orderService.cancelOrder(orderId)
            );

            assertNotNull(exception);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void cancelOrder_WhenNotFound_ShouldThrowException() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            OrderNotFoundException exception = assertThrows(
                    OrderNotFoundException.class,
                    () -> orderService.cancelOrder(orderId)
            );

            assertNotNull(exception);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    // ========================================
    // COMPLETE ORDER TESTS
    // ========================================

    @Nested
    @DisplayName("completeOrder Tests")
    class CompleteOrderTests {

        @Test
        @DisplayName("Should successfully complete pending order")
        void completeOrder_WhenPending_ShouldSucceed() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order mockOrder = TestDataBuilder.buildOrderWithStatus(Status.PENDING);
            mockOrder.setOrderId(orderId);

            Order completedOrder = TestDataBuilder.buildOrderWithStatus(Status.COMPLETED);
            completedOrder.setOrderId(orderId);

            OrderResponseDTO responseDTO = TestDataBuilder.buildOrderResponseDTO();
            responseDTO.setOrderId(orderId);
            responseDTO.setOrderStatus(Status.COMPLETED);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(completedOrder);
            when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);

            // Act
            OrderResponseDTO result = orderService.completeOrder(orderId);

            // Assert
            assertNotNull(result);
            assertEquals(Status.COMPLETED, result.getOrderStatus());
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when completing cancelled order")
        void completeOrder_WhenCancelled_ShouldThrowException() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order cancelledOrder = TestDataBuilder.buildOrderWithStatus(Status.CANCELLED);
            cancelledOrder.setOrderId(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(cancelledOrder));

            // Act & Assert
            InvalidOrderStateException exception = assertThrows(
                    InvalidOrderStateException.class,
                    () -> orderService.completeOrder(orderId)
            );

            assertNotNull(exception);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when completing already completed order")
        void completeOrder_WhenAlreadyCompleted_ShouldThrowException() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order completedOrder = TestDataBuilder.buildOrderWithStatus(Status.COMPLETED);
            completedOrder.setOrderId(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

            // Act & Assert
            InvalidOrderStateException exception = assertThrows(
                    InvalidOrderStateException.class,
                    () -> orderService.completeOrder(orderId)
            );

            assertNotNull(exception);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void completeOrder_WhenNotFound_ShouldThrowException() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            OrderNotFoundException exception = assertThrows(
                    OrderNotFoundException.class,
                    () -> orderService.completeOrder(orderId)
            );

            assertNotNull(exception);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any(Order.class));
        }
    }
}
