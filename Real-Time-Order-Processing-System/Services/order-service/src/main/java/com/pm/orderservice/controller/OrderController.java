package com.pm.orderservice.controller;

import com.pm.orderservice.dto.OrderRequestDTO;
import com.pm.orderservice.dto.OrderResponseDTO;
import com.pm.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/orders")
@Tag(name = "Order Controller", description = "Operations pertaining to orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create Order", description = "Create a new order")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO orderRequestDTO){
        return ResponseEntity.ok(orderService.createOrder(orderRequestDTO));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Get users order by order ID")
    public ResponseEntity<OrderResponseDTO> getOrderById(@Valid @PathVariable UUID orderId){
        OrderResponseDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @Operation(summary = "List Orders", description = "Listing orders using pagination")
    public ResponseEntity<Page<OrderResponseDTO>> listOrders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Page<OrderResponseDTO> orders = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer orders", description = "Getting customer orders via their ID")
    public ResponseEntity<List<OrderResponseDTO>> getCustomerOrders(@PathVariable UUID customerId){
        List<OrderResponseDTO> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel Order", description = "Cancel an order")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable UUID orderId){
        OrderResponseDTO order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(order);
    }

}
