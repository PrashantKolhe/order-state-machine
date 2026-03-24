package com.gametime.challenge.orders.integration;

import com.gametime.challenge.orders.domain.Order;

/**
 * Abstraction around downstream payment-related behavior.
 */
public interface PaymentGateway {

    /**
     * Attempts to authorize payment for a new order.
     */
    PaymentAuthorizationResult authorizePayment(Order order, PaymentScenario scenario);

    /**
     * Attempts the completion step for an already-authorized order.
     */
    PaymentCompletionResult completeAuthorizedOrder(Order order, PaymentScenario scenario);

    /**
     * Attempts to void a previously-authorized payment.
     */
    PaymentVoidResult voidPayment(Order order, PaymentScenario scenario);
}
