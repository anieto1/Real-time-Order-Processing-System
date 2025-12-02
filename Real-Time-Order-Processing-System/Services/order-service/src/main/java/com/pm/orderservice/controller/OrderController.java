package com.pm.orderservice.controller;

import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/orders")
@Tag(name = "Order Controller", description = "Operations pertaining to orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/orders")
    @Operation(summary = "Create Order", description = "Create a new order")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO orderRequestDTO){
        return ResponseEntity.ok(orderService.createOrder(orderRequestDTO));
    }

    @GetMapping("/api/orders/{orderId}")
    @Operation(summary = "Get order by ID", description = "Get users order by order ID")
    public ResponseEntity<OrderResponseDTO> getOrderById(@Valid @PathVariable UUID orderId){
        OrderResponseDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/api/orders")
    @Operation(summary = "List Orders", description = "Listing orders using pagination")
    public ResponseEntity<Iterable<OrderResponseDTO>> listOrders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Iterable<OrderResponseDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/api/orders/customer/{customerId}")
    @Operation(summary = "Get customer orders", description = "Getting customer orders via their ID")
    public ResponseEntity<Iterable<OrderResponseDTO>> getCustomerOrders(@PathVariable UUID customerId){
        Iterable<OrderResponseDTO> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/api/orders/{orderId}/cancel")
    @Operation(summary = "Cancel Order", description = "Cancel an order")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable UUID orderId){
        OrderResponseDTO order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(order);
    }

}
