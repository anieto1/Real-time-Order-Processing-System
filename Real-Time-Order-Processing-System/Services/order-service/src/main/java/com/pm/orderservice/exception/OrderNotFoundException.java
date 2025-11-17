package com.pm.orderservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends OrderServiceException {

    public OrderNotFoundException(UUID orderId) {
        super(String.format("Order not found with ID: %s", orderId));
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
