package com.pm.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.orderservice.dto.OrderItemRequestDTO;
import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.exception.InvalidOrderStateException;
import com.pm.orderservice.exception.OrderNotFoundException;
import com.pm.orderservice.model.Status;
import com.pm.orderservice.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    // ========================================
    // HELPER METHODS
    // ========================================

    private OrderRequestDTO createValidOrderRequest() {
        return OrderRequestDTO.builder()
                .customerId(UUID.randomUUID())
                .orderItems(List.of(
                        OrderItemRequestDTO.builder()
                                .productId(UUID.randomUUID())
                                .quantity(2)
                                .price(new BigDecimal("29.99"))
                                .build()
                ))
                .build();
    }

    private OrderResponseDTO createOrderResponse() {
        return OrderResponseDTO.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .orderStatus(Status.PENDING)
                .totalAmount(new BigDecimal("59.98"))
                .orderItems(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========================================
    // POST /api/orders - CREATE ORDER TESTS
    // ========================================

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully with valid request")
        void createOrder_WithValidRequest_ShouldReturn200() throws Exception {
            // Arrange
            OrderRequestDTO request = createValidOrderRequest();
            OrderResponseDTO response = createOrderResponse();

            when(orderService.createOrder(any(OrderRequestDTO.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").exists())
                    .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").exists());

            verify(orderService, times(1)).createOrder(any(OrderRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when customerId is null")
        void createOrder_WithNullCustomerId_ShouldReturn400() throws Exception {
            // Arrange
            OrderRequestDTO request = OrderRequestDTO.builder()
                    .customerId(null)  // Missing customer ID
                    .orderItems(List.of(
                            OrderItemRequestDTO.builder()
                                    .productId(UUID.randomUUID())
                                    .quantity(1)
                                    .price(new BigDecimal("10.00"))
                                    .build()
                    ))
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should return 400 when orderItems is empty")
        void createOrder_WithEmptyItems_ShouldReturn400() throws Exception {
            // Arrange
            OrderRequestDTO request = OrderRequestDTO.builder()
                    .customerId(UUID.randomUUID())
                    .orderItems(Collections.emptyList())
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should return 400 when item quantity is less than 1")
        void createOrder_WithInvalidQuantity_ShouldReturn400() throws Exception {
            // Arrange
            OrderRequestDTO request = OrderRequestDTO.builder()
                    .customerId(UUID.randomUUID())
                    .orderItems(List.of(
                            OrderItemRequestDTO.builder()
                                    .productId(UUID.randomUUID())
                                    .quantity(0)  // Invalid quantity
                                    .price(new BigDecimal("10.00"))
                                    .build()
                    ))
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should return 400 when price is zero or negative")
        void createOrder_WithInvalidPrice_ShouldReturn400() throws Exception {
            // Arrange
            OrderRequestDTO request = OrderRequestDTO.builder()
                    .customerId(UUID.randomUUID())
                    .orderItems(List.of(
                            OrderItemRequestDTO.builder()
                                    .productId(UUID.randomUUID())
                                    .quantity(1)
                                    .price(new BigDecimal("0.00"))  // Invalid price
                                    .build()
                    ))
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should return 400 when productId is null")
        void createOrder_WithNullProductId_ShouldReturn400() throws Exception {
            // Arrange
            OrderRequestDTO request = OrderRequestDTO.builder()
                    .customerId(UUID.randomUUID())
                    .orderItems(List.of(
                            OrderItemRequestDTO.builder()
                                    .productId(null)  // Missing product ID
                                    .quantity(1)
                                    .price(new BigDecimal("10.00"))
                                    .build()
                    ))
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).createOrder(any());
        }
    }

    // ========================================
    // GET /api/orders/{orderId} - GET ORDER BY ID TESTS
    // ========================================

    @Nested
    @DisplayName("GET /api/orders/{orderId}")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order when found")
        void getOrderById_WhenExists_ShouldReturn200() throws Exception {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderResponseDTO response = createOrderResponse();
            response.setOrderId(orderId);

            when(orderService.getOrderById(orderId)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/orders/{orderId}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$.orderStatus").exists());

            verify(orderService, times(1)).getOrderById(orderId);
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void getOrderById_WhenNotExists_ShouldReturn404() throws Exception {
            // Arrange
            UUID orderId = UUID.randomUUID();

            when(orderService.getOrderById(orderId))
                    .thenThrow(new OrderNotFoundException(orderId));

            // Act & Assert
            mockMvc.perform(get("/api/orders/{orderId}", orderId))
                    .andExpect(status().isNotFound());

            verify(orderService, times(1)).getOrderById(orderId);
        }

        @Test
        @DisplayName("Should return 500 when orderId is invalid UUID format (no specific handler)")
        void getOrderById_WithInvalidUUID_ShouldReturn500() throws Exception {
            // Act & Assert - Invalid UUID format falls through to generic exception handler
            mockMvc.perform(get("/api/orders/{orderId}", "not-a-uuid"))
                    .andExpect(status().isInternalServerError());

            verify(orderService, never()).getOrderById(any());
        }
    }

    // ========================================
    // GET /api/orders - LIST ORDERS TESTS
    // ========================================

    @Nested
    @DisplayName("GET /api/orders")
    class ListOrdersTests {

        @Test
        @DisplayName("Should return paginated orders")
        void listOrders_ShouldReturnPaginatedResults() throws Exception {
            // Arrange
            OrderResponseDTO response = createOrderResponse();
            Page<OrderResponseDTO> page = new PageImpl<>(List.of(response));

            when(orderService.getAllOrders(anyInt(), anyInt())).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/orders")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(orderService, times(1)).getAllOrders(0, 10);
        }

        @Test
        @DisplayName("Should return empty page when no orders exist")
        void listOrders_WhenEmpty_ShouldReturnEmptyPage() throws Exception {
            // Arrange
            Page<OrderResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());

            when(orderService.getAllOrders(anyInt(), anyInt())).thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should use default pagination when params not provided")
        void listOrders_WithoutParams_ShouldUseDefaults() throws Exception {
            // Arrange
            Page<OrderResponseDTO> page = new PageImpl<>(Collections.emptyList());

            when(orderService.getAllOrders(0, 10)).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk());

            verify(orderService, times(1)).getAllOrders(0, 10);
        }
    }

    // ========================================
    // GET /api/orders/customer/{customerId} - GET CUSTOMER ORDERS TESTS
    // ========================================

    @Nested
    @DisplayName("GET /api/orders/customer/{customerId}")
    class GetCustomerOrdersTests {

        @Test
        @DisplayName("Should return orders for customer")
        void getCustomerOrders_WhenOrdersExist_ShouldReturn200() throws Exception {
            // Arrange
            UUID customerId = UUID.randomUUID();
            OrderResponseDTO response = createOrderResponse();
            response.setCustomerId(customerId);

            when(orderService.getOrdersByCustomerId(customerId))
                    .thenReturn(List.of(response));

            // Act & Assert
            mockMvc.perform(get("/api/orders/customer/{customerId}", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(orderService, times(1)).getOrdersByCustomerId(customerId);
        }

        @Test
        @DisplayName("Should return empty list when customer has no orders")
        void getCustomerOrders_WhenNoOrders_ShouldReturnEmptyList() throws Exception {
            // Arrange
            UUID customerId = UUID.randomUUID();

            when(orderService.getOrdersByCustomerId(customerId))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/orders/customer/{customerId}", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ========================================
    // PATCH /api/orders/{orderId}/cancel - CANCEL ORDER TESTS
    // ========================================

    @Nested
    @DisplayName("PATCH /api/orders/{orderId}/cancel")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order successfully")
        void cancelOrder_WhenValid_ShouldReturn200() throws Exception {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderResponseDTO response = createOrderResponse();
            response.setOrderId(orderId);
            response.setOrderStatus(Status.CANCELLED);

            when(orderService.cancelOrder(orderId)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

            verify(orderService, times(1)).cancelOrder(orderId);
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void cancelOrder_WhenNotFound_ShouldReturn404() throws Exception {
            // Arrange
            UUID orderId = UUID.randomUUID();

            when(orderService.cancelOrder(orderId))
                    .thenThrow(new OrderNotFoundException(orderId));

            // Act & Assert
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId))
                    .andExpect(status().isNotFound());

            verify(orderService, times(1)).cancelOrder(orderId);
        }

        @Test
        @DisplayName("Should return 409 when order already completed")
        void cancelOrder_WhenCompleted_ShouldReturn409() throws Exception {
            // Arrange
            UUID orderId = UUID.randomUUID();

            when(orderService.cancelOrder(orderId))
                    .thenThrow(new InvalidOrderStateException(orderId, Status.COMPLETED, "cancel"));

            // Act & Assert - InvalidOrderStateException returns 409 Conflict
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId))
                    .andExpect(status().isConflict());

            verify(orderService, times(1)).cancelOrder(orderId);
        }

        @Test
        @DisplayName("Should return 409 when order already cancelled")
        void cancelOrder_WhenAlreadyCancelled_ShouldReturn409() throws Exception {
            // Arrange
            UUID orderId = UUID.randomUUID();

            when(orderService.cancelOrder(orderId))
                    .thenThrow(new InvalidOrderStateException(orderId, Status.CANCELLED, "cancel"));

            // Act & Assert - InvalidOrderStateException returns 409 Conflict
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId))
                    .andExpect(status().isConflict());

            verify(orderService, times(1)).cancelOrder(orderId);
        }
    }
}
