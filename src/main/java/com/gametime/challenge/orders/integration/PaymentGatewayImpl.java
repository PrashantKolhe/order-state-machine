package com.gametime.challenge.orders.integration;

import com.gametime.challenge.orders.domain.Order;
import org.springframework.stereotype.Component;

/**
 * In-memory gateway stub that uses request scenarios to drive downstream outcomes.
 */
@Component
public class PaymentGatewayImpl implements PaymentGateway {

    @Override
    public PaymentAuthorizationResult authorizePayment(Order order, PaymentScenario scenario) {
        if (scenario == PaymentScenario.AUTHORIZATION_DECLINED) {
            return new PaymentAuthorizationResult(
                    PaymentAuthorizationResult.AuthorizationStatus.DECLINED,
                    "Payment authorization was declined.");
        }

        return new PaymentAuthorizationResult(
                PaymentAuthorizationResult.AuthorizationStatus.APPROVED,
                "Payment authorization succeeded.");
    }

    @Override
    public PaymentCompletionResult completeAuthorizedOrder(Order order, PaymentScenario scenario) {
        if (scenario == PaymentScenario.COMPLETION_FAILED_VOID_SUCCESS
                || scenario == PaymentScenario.COMPLETION_FAILED_VOID_FAILED) {
            return new PaymentCompletionResult(
                    PaymentCompletionResult.CompletionStatus.FAILED,
                    "Order completion failed after payment authorization.");
        }

        return new PaymentCompletionResult(
                PaymentCompletionResult.CompletionStatus.SUCCEEDED,
                "Order completed successfully.");
    }

    @Override
    public PaymentVoidResult voidPayment(Order order, PaymentScenario scenario) {
        if (scenario == PaymentScenario.COMPLETION_FAILED_VOID_FAILED) {
            return new PaymentVoidResult(
                    PaymentVoidResult.VoidStatus.FAILED,
                    "Payment void failed; manual follow-up is required.");
        }

        return new PaymentVoidResult(
                PaymentVoidResult.VoidStatus.SUCCEEDED,
                "Authorized payment was voided successfully.");
    }
}
