package com.gametime.challenge.orders.exception;

import com.gametime.challenge.orders.domain.OrderState;
import java.util.UUID;

/**
 * Raised when a request attempts an unsupported state transition.
 */
public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(UUID orderId, OrderState currentState, String details) {
        super("Order " + orderId + " is in state " + currentState + ". " + details);
    }
}
