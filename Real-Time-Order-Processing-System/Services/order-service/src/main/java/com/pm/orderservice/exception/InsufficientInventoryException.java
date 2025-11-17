package com.pm.orderservice.exception;

import java.util.UUID;

public class InsufficientInventoryException extends OrderServiceException {

    public InsufficientInventoryException(UUID productId, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient inventory for product %s. Requested: %d, Available: %d",
                productId, requestedQuantity, availableQuantity));
    }

    public InsufficientInventoryException(UUID productId) {
        super(String.format("Product %s is out of stock", productId));
    }

    public InsufficientInventoryException(String message) {
        super(message);
    }
}
