package com.pm.orderservice.exception;

import java.util.UUID;

public class ProductNotFoundException extends OrderServiceException {

    public ProductNotFoundException(UUID productId) {
        super(String.format("Product not found with ID: %s", productId));
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}
