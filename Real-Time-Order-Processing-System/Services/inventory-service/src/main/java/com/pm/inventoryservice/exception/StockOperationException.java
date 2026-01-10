package com.pm.inventoryservice.exception;

public class StockOperationException extends RuntimeException {
    public StockOperationException(String message) {
        super(message);
    }
}
