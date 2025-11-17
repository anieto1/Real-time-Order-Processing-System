package com.pm.orderservice.exception;

public class InvalidOrderException extends OrderServiceException {

    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
