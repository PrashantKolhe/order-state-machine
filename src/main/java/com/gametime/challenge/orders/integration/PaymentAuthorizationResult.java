package com.gametime.challenge.orders.integration;

/**
 * Result of a payment authorization attempt.
 */
public class PaymentAuthorizationResult {
    private final AuthorizationStatus status;
    private final String message;

    public PaymentAuthorizationResult(AuthorizationStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public AuthorizationStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isApproved() {
        return status == AuthorizationStatus.APPROVED;
    }

    public enum AuthorizationStatus {
        APPROVED,
        DECLINED
    }
}
