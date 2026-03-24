package com.gametime.challenge.orders.integration;

/**
 * Result of the downstream completion step.
 */
public class PaymentCompletionResult {
    private final CompletionStatus status;
    private final String message;

    public PaymentCompletionResult(CompletionStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public CompletionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return status == CompletionStatus.SUCCEEDED;
    }

    public enum CompletionStatus {
        SUCCEEDED,
        FAILED
    }
}
