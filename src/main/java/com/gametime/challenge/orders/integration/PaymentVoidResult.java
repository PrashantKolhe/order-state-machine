package com.gametime.challenge.orders.integration;

/**
 * Result of a void attempt for an already-authorized payment.
 */
public class PaymentVoidResult {
    private final VoidStatus status;
    private final String message;

    public PaymentVoidResult(VoidStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public VoidStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return status == VoidStatus.SUCCEEDED;
    }

    public enum VoidStatus {
        SUCCEEDED,
        FAILED
    }
}
