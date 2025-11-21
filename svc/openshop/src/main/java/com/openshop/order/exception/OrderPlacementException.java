package com.openshop.order.exception;

/**
 * Exception thrown when order placement fails
 */
public class OrderPlacementException extends RuntimeException {
    public OrderPlacementException(String message) {
        super(message);
    }

    public OrderPlacementException(String message, Throwable cause) {
        super(message, cause);
    }
}
