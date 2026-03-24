package com.gametime.challenge.orders.exception;

import java.util.UUID;

/**
 * Raised when the requested order does not exist.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Order " + orderId + " was not found.");
    }
}
