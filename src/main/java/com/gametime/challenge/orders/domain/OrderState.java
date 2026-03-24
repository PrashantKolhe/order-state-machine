package com.gametime.challenge.orders.domain;

/**
 * Supported states for the order state machine.
 */
public enum OrderState {
    INITIALIZED,
    PAYMENT_AUTHORIZED,
    COMPLETE,
    REJECTED,
    CANCELLED,
    NEEDS_ATTENTION;

    public boolean canTransitionTo(OrderState targetState) {
        return switch (this) {
            case INITIALIZED -> targetState == PAYMENT_AUTHORIZED || targetState == REJECTED;
            case PAYMENT_AUTHORIZED -> targetState == COMPLETE
                    || targetState == CANCELLED
                    || targetState == NEEDS_ATTENTION;
            case COMPLETE, REJECTED, CANCELLED, NEEDS_ATTENTION -> false;
        };
    }
}
