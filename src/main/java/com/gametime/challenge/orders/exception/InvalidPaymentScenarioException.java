package com.gametime.challenge.orders.exception;

/**
 * Raised when the caller provides an unsupported payment scenario header value.
 */
public class InvalidPaymentScenarioException extends RuntimeException {

    public InvalidPaymentScenarioException(String message) {
        super(message);
    }
}
