package com.pm.orderservice.util;

import com.pm.orderservice.dto.OrderItemRequestDTO;
import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.model.Order;
import com.pm.orderservice.model.OrderItem;
import com.pm.orderservice.model.OutboxEvent;
import com.pm.orderservice.model.EventType;
import com.pm.orderservice.model.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test Data Builder - Utility class to create test data easily
 *
 * WHY THIS EXISTS:
 * Instead of writing the same setup code in every test, we create reusable builders here.
 * This makes tests cleaner and easier to write!
 *
 * USAGE EXAMPLE:
 * OrderRequestDTO request = TestDataBuilder.buildValidOrderRequest();
 * Order order = TestDataBuilder.buildOrder();
 */
public class TestDataBuilder {

    // ======================
    // ORDER REQUEST DTOs (for API requests)
    // ======================

    /**
     * Creates a valid order request with 2 items
     * Use this for "happy path" tests
     */
    public static OrderRequestDTO buildValidOrderRequest() {
        return OrderRequestDTO.builder()
                .customerId(UUID.randomUUID())
                .orderItems(List.of(
                        buildOrderItemRequest("PRODUCT-1", 2, new BigDecimal("29.99")),
                        buildOrderItemRequest("PRODUCT-2", 1, new BigDecimal("49.99"))
                ))
                .build();
    }

    /**
     * Creates an order request with empty items list
     * Use this for validation tests
     */
    public static OrderRequestDTO buildOrderRequestWithEmptyItems() {
        return OrderRequestDTO.builder()
                .customerId(UUID.randomUUID())
                .orderItems(new ArrayList<>())  // Empty list
                .build();
    }

    /**
     * Creates an order request with null items
     * Use this for null validation tests
     */
    public static OrderRequestDTO buildOrderRequestWithNullItems() {
        return OrderRequestDTO.builder()
                .customerId(UUID.randomUUID())
                .orderItems(null)  // Null items
                .build();
    }

    /**
     * Creates a single order item request
     */
    public static OrderItemRequestDTO buildOrderItemRequest(String productId, int quantity, BigDecimal price) {
        return OrderItemRequestDTO.builder()
                .productId(UUID.fromString(productId.hashCode() % 2 == 0
                    ? "00000000-0000-0000-0000-000000000001"
                    : "00000000-0000-0000-0000-000000000002"))
                .quantity(quantity)
                .price(price)
                .build();
    }

    // ======================
    // ORDER ENTITIES (domain models)
    // ======================

    /**
     * Creates a complete Order entity with items
     * Use this when you need a full order object for testing
     */
    public static Order buildOrder() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Order order = Order.builder()
                .orderId(orderId)
                .customerId(customerId)
                .orderStatus(Status.PENDING)
                .totalAmount(new BigDecimal("109.97"))  // 29.99*2 + 49.99
                .orderItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0L)
                .build();

        // Add order items
        OrderItem item1 = buildOrderItem(order, UUID.randomUUID(), 2, new BigDecimal("29.99"));
        OrderItem item2 = buildOrderItem(order, UUID.randomUUID(), 1, new BigDecimal("49.99"));

        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);

        return order;
    }

    /**
     * Creates a single OrderItem entity
     */
    public static OrderItem buildOrderItem(Order order, UUID productId, int quantity, BigDecimal price) {
        return OrderItem.builder()
                .itemId(UUID.randomUUID())
                .order(order)
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .build();
    }

    // ======================
    // ORDER RESPONSE DTOs (for API responses)
    // ======================

    /**
     * Creates an OrderResponseDTO
     * Use this when mocking service responses
     */
    public static OrderResponseDTO buildOrderResponseDTO() {
        return OrderResponseDTO.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .orderStatus(Status.PENDING)
                .totalAmount(new BigDecimal("109.97"))
                .orderItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ======================
    // OUTBOX EVENTS
    // ======================

    /**
     * Creates an OutboxEvent for testing
     */
    public static OutboxEvent buildOutboxEvent(UUID orderId) {
        return OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateId(orderId)
                .aggregateType("ORDER")
                .eventType(EventType.ORDER_CREATED)
                .payload("{\"orderId\":\"" + orderId + "\"}")
                .published(false)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ======================
    // CUSTOM BUILDERS (with specific values)
    // ======================

    /**
     * Creates an order with a specific customer ID
     * Useful for testing customer-specific queries
     */
    public static Order buildOrderForCustomer(UUID customerId) {
        Order order = buildOrder();
        order.setCustomerId(customerId);
        return order;
    }

    /**
     * Creates an order with a specific status
     */
    public static Order buildOrderWithStatus(Status status) {
        Order order = buildOrder();
        order.setOrderStatus(status);
        return order;
    }
}
