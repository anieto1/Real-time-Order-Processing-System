package com.pm.orderservice.exception;

import com.pm.orderservice.model.Status;

import java.util.UUID;

public class InvalidOrderStateException extends OrderServiceException {

    public InvalidOrderStateException(UUID orderId, Status currentStatus, String operation) {
        super(String.format("Cannot perform operation '%s' on order %s with status: %s",
                operation, orderId, currentStatus));
    }

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
