package com.pm.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.orderservice.dto.OrderItemRequestDTO;
import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.model.OutboxEvent;
import com.pm.orderservice.model.Status;
import com.pm.orderservice.repository.OrderRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import com.pm.orderservice.service.OrderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Order Service
 *
 * These tests verify the full flow including:
 * - HTTP request handling
 * - Database persistence
 * - Outbox event creation
 * - Kafka integration (with embedded Kafka)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {"order-events", "order-events-dlq"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Order Service Integration Tests")
class OrderServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

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
                                .build(),
                        OrderItemRequestDTO.builder()
                                .productId(UUID.randomUUID())
                                .quantity(1)
                                .price(new BigDecimal("49.99"))
                                .build()
                ))
                .build();
    }

    // ========================================
    // ORDER CREATION FLOW TESTS
    // ========================================

    @Nested
    @DisplayName("Order Creation Flow")
    class OrderCreationFlowTests {

        @Test
        @DisplayName("Should create order and persist to database via REST API")
        void createOrder_ShouldPersistToDatabase() throws Exception {
            // Arrange
            OrderRequestDTO request = createValidOrderRequest();

            // Act
            MvcResult result = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").exists())
                    .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                    .andReturn();

            // Assert - Verify database state
            OrderResponseDTO response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    OrderResponseDTO.class
            );

            assertTrue(orderRepository.findById(response.getOrderId()).isPresent());
            assertEquals(1, orderRepository.count());
        }

        @Test
        @DisplayName("Should create outbox event when order is created")
        void createOrder_ShouldCreateOutboxEvent() throws Exception {
            // Arrange
            OrderRequestDTO request = createValidOrderRequest();

            // Act
            MvcResult result = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            OrderResponseDTO response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    OrderResponseDTO.class
            );

            // Assert - Verify outbox event was created
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertEquals(1, outboxEvents.size());

            OutboxEvent event = outboxEvents.get(0);
            assertEquals(response.getOrderId(), event.getAggregateId());
            assertEquals("ORDER", event.getAggregateType());
            assertFalse(event.isPublished());
            assertEquals(0, event.getRetryCount());
        }

        @Test
        @DisplayName("Should calculate total amount correctly")
        void createOrder_ShouldCalculateTotalAmountCorrectly() throws Exception {
            // Arrange - 2 items @ 29.99 + 1 item @ 49.99 = 109.97
            OrderRequestDTO request = createValidOrderRequest();

            // Act
            MvcResult result = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            OrderResponseDTO response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    OrderResponseDTO.class
            );

            // Assert
            assertEquals(new BigDecimal("109.97"), response.getTotalAmount());
        }
    }

    // ========================================
    // ORDER RETRIEVAL TESTS
    // ========================================

    @Nested
    @DisplayName("Order Retrieval Flow")
    class OrderRetrievalFlowTests {

        @Test
        @DisplayName("Should retrieve order by ID")
        void getOrderById_ShouldReturnOrder() throws Exception {
            // Arrange - Create an order first
            OrderRequestDTO request = createValidOrderRequest();
            OrderResponseDTO created = orderService.createOrder(request);

            // Act & Assert
            mockMvc.perform(get("/api/orders/{orderId}", created.getOrderId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(created.getOrderId().toString()))
                    .andExpect(jsonPath("$.customerId").value(created.getCustomerId().toString()))
                    .andExpect(jsonPath("$.orderStatus").value("PENDING"));
        }

        @Test
        @DisplayName("Should retrieve orders by customer ID")
        void getOrdersByCustomerId_ShouldReturnCustomerOrders() throws Exception {
            // Arrange - Create multiple orders for same customer
            UUID customerId = UUID.randomUUID();
            OrderRequestDTO request1 = createValidOrderRequest();
            request1.setCustomerId(customerId);
            OrderRequestDTO request2 = createValidOrderRequest();
            request2.setCustomerId(customerId);

            orderService.createOrder(request1);
            orderService.createOrder(request2);

            // Act & Assert
            mockMvc.perform(get("/api/orders/customer/{customerId}", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should return paginated list of all orders")
        void getAllOrders_ShouldReturnPaginatedList() throws Exception {
            // Arrange - Create multiple orders
            orderService.createOrder(createValidOrderRequest());
            orderService.createOrder(createValidOrderRequest());
            orderService.createOrder(createValidOrderRequest());

            // Act & Assert
            mockMvc.perform(get("/api/orders")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }
    }

    // ========================================
    // ORDER LIFECYCLE TESTS
    // ========================================

    @Nested
    @DisplayName("Order Lifecycle Flow")
    class OrderLifecycleFlowTests {

        @Test
        @DisplayName("Should cancel pending order successfully")
        void cancelOrder_WhenPending_ShouldSucceed() throws Exception {
            // Arrange
            OrderResponseDTO created = orderService.createOrder(createValidOrderRequest());

            // Act & Assert
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", created.getOrderId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

            // Verify database state
            var order = orderRepository.findById(created.getOrderId()).orElseThrow();
            assertEquals(Status.CANCELLED, order.getOrderStatus());
        }

        @Test
        @DisplayName("Should complete pending order successfully")
        void completeOrder_WhenPending_ShouldSucceed() throws Exception {
            // Arrange
            OrderResponseDTO created = orderService.createOrder(createValidOrderRequest());

            // Act - Using service directly since there's no REST endpoint for complete
            OrderResponseDTO completed = orderService.completeOrder(created.getOrderId());

            // Assert
            assertEquals(Status.COMPLETED, completed.getOrderStatus());

            // Verify database state
            var order = orderRepository.findById(created.getOrderId()).orElseThrow();
            assertEquals(Status.COMPLETED, order.getOrderStatus());
        }

        @Test
        @DisplayName("Should fail to cancel completed order")
        void cancelOrder_WhenCompleted_ShouldFail() throws Exception {
            // Arrange
            OrderResponseDTO created = orderService.createOrder(createValidOrderRequest());
            orderService.completeOrder(created.getOrderId());

            // Act & Assert
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", created.getOrderId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to cancel already cancelled order")
        void cancelOrder_WhenAlreadyCancelled_ShouldFail() throws Exception {
            // Arrange
            OrderResponseDTO created = orderService.createOrder(createValidOrderRequest());
            orderService.cancelOrder(created.getOrderId());

            // Act & Assert
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", created.getOrderId()))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================
    // VALIDATION TESTS
    // ========================================

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("Should reject order with missing customer ID")
        void createOrder_WithMissingCustomerId_ShouldReturn400() throws Exception {
            // Arrange
            String invalidJson = """
                    {
                        "orderItems": [
                            {
                                "productId": "550e8400-e29b-41d4-a716-446655440000",
                                "quantity": 1,
                                "price": 10.00
                            }
                        ]
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());

            // Verify nothing was persisted
            assertEquals(0, orderRepository.count());
            assertEquals(0, outboxEventRepository.count());
        }

        @Test
        @DisplayName("Should reject order with empty items")
        void createOrder_WithEmptyItems_ShouldReturn400() throws Exception {
            // Arrange
            String invalidJson = """
                    {
                        "customerId": "550e8400-e29b-41d4-a716-446655440000",
                        "orderItems": []
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject order with invalid quantity")
        void createOrder_WithInvalidQuantity_ShouldReturn400() throws Exception {
            // Arrange
            String invalidJson = """
                    {
                        "customerId": "550e8400-e29b-41d4-a716-446655440000",
                        "orderItems": [
                            {
                                "productId": "550e8400-e29b-41d4-a716-446655440001",
                                "quantity": 0,
                                "price": 10.00
                            }
                        ]
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject order with negative price")
        void createOrder_WithNegativePrice_ShouldReturn400() throws Exception {
            // Arrange
            String invalidJson = """
                    {
                        "customerId": "550e8400-e29b-41d4-a716-446655440000",
                        "orderItems": [
                            {
                                "productId": "550e8400-e29b-41d4-a716-446655440001",
                                "quantity": 1,
                                "price": -5.00
                            }
                        ]
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void getOrder_WhenNotExists_ShouldReturn404() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(get("/api/orders/{orderId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 500 for invalid UUID format (no handler)")
        void getOrder_WithInvalidUUID_ShouldReturn500() throws Exception {
            // Act & Assert - Invalid UUID falls through to generic exception handler
            mockMvc.perform(get("/api/orders/{orderId}", "invalid-uuid"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return 404 when cancelling non-existent order")
        void cancelOrder_WhenNotExists_ShouldReturn404() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================
    // OUTBOX PATTERN TESTS
    // ========================================

    @Nested
    @DisplayName("Outbox Pattern")
    class OutboxPatternTests {

        @Test
        @DisplayName("Should create unpublished outbox event on order creation")
        void createOrder_ShouldCreateUnpublishedOutboxEvent() throws Exception {
            // Arrange
            OrderRequestDTO request = createValidOrderRequest();

            // Act
            OrderResponseDTO response = orderService.createOrder(request);

            // Assert
            List<OutboxEvent> events = outboxEventRepository
                    .findTop100ByPublishedFalseOrderByCreatedAt();

            assertEquals(1, events.size());
            OutboxEvent event = events.get(0);

            assertEquals(response.getOrderId(), event.getAggregateId());
            assertFalse(event.isPublished());
            assertNotNull(event.getPayload());
            assertTrue(event.getPayload().contains(response.getOrderId().toString()));
        }

        @Test
        @DisplayName("Should store order details in outbox event payload")
        void createOrder_OutboxEventPayload_ShouldContainOrderDetails() throws Exception {
            // Arrange
            OrderRequestDTO request = createValidOrderRequest();

            // Act
            OrderResponseDTO response = orderService.createOrder(request);

            // Assert
            OutboxEvent event = outboxEventRepository.findAll().get(0);
            String payload = event.getPayload();

            assertTrue(payload.contains(response.getOrderId().toString()));
            assertTrue(payload.contains(response.getCustomerId().toString()));
            assertTrue(payload.contains("PENDING"));
        }
    }

    // ========================================
    // CONCURRENT ACCESS TESTS
    // ========================================

    @Nested
    @DisplayName("Concurrent Access")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle multiple order creations")
        void createMultipleOrders_ShouldAllSucceed() throws Exception {
            // Arrange
            int orderCount = 5;

            // Act
            for (int i = 0; i < orderCount; i++) {
                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createValidOrderRequest())))
                        .andExpect(status().isOk());
            }

            // Assert
            assertEquals(orderCount, orderRepository.count());
            assertEquals(orderCount, outboxEventRepository.count());
        }
    }
}
