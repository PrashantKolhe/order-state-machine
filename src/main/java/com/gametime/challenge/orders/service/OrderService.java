package com.gametime.challenge.orders.service;

import com.gametime.challenge.orders.domain.Order;
import com.gametime.challenge.orders.domain.OrderState;
import com.gametime.challenge.orders.domain.OrderStateHistory;
import com.gametime.challenge.orders.dto.CreateOrderRequest;
import com.gametime.challenge.orders.dto.PaymentRequest;
import com.gametime.challenge.orders.exception.InvalidOrderStateTransitionException;
import com.gametime.challenge.orders.exception.OrderNotFoundException;
import com.gametime.challenge.orders.integration.PaymentAuthorizationResult;
import com.gametime.challenge.orders.integration.PaymentCompletionResult;
import com.gametime.challenge.orders.integration.PaymentGateway;
import com.gametime.challenge.orders.integration.PaymentScenario;
import com.gametime.challenge.orders.integration.PaymentVoidResult;
import com.gametime.challenge.orders.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coordinates order creation, state transitions, and recovery behavior.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    /**
     * Creates a new order in the INITIALIZED state.
     */
    public Order createOrder(CreateOrderRequest request) {
        Instant now = Instant.now();
        Order order = new Order(
                UUID.randomUUID(),
                request.getEventId().trim(),
                request.getAmountInCents(),
                request.getCurrency(),
                now);
        return orderRepository.save(order);
    }

    /**
     * Attempts payment authorization and transitions the order based on the outcome.
     */
    public Order authorizeOrder(UUID orderId, PaymentRequest paymentRequest, PaymentScenario scenario) {
        Order order = getExistingOrder(orderId);
        ensureCanAuthorize(order);
        order.updateLast4(deriveLast4(paymentRequest.getCardNumber()));

        PaymentAuthorizationResult authorizationResult = paymentGateway.authorizePayment(order, scenario);
        if (authorizationResult.isApproved()) {
            order.transitionTo(OrderState.PAYMENT_AUTHORIZED, now(), authorizationResult.getMessage());
        } else {
            order.transitionTo(OrderState.REJECTED, now(), authorizationResult.getMessage());
        }

        return orderRepository.save(order);
    }

    /**
     * Completes an authorized order or runs recovery when completion fails.
     */
    public Order completeOrder(UUID orderId, PaymentScenario scenario) {
        Order order = getExistingOrder(orderId);
        ensureCanComplete(order);

        PaymentCompletionResult completionResult = paymentGateway.completeAuthorizedOrder(order, scenario);
        if (completionResult.isSuccessful()) {
            order.transitionTo(OrderState.COMPLETE, now(), completionResult.getMessage());
            return orderRepository.save(order);
        }

        PaymentVoidResult voidResult = paymentGateway.voidPayment(order, scenario);
        if (voidResult.isSuccessful()) {
            order.transitionTo(
                    OrderState.CANCELLED,
                    now(),
                    completionResult.getMessage() + " " + voidResult.getMessage());
        } else {
            order.transitionTo(
                    OrderState.NEEDS_ATTENTION,
                    now(),
                    completionResult.getMessage() + " " + voidResult.getMessage());
        }

        return orderRepository.save(order);
    }

    /**
     * Returns the current order snapshot.
     */
    public Order getOrder(UUID orderId) {
        return getExistingOrder(orderId);
    }

    /**
     * Returns state history in chronological order.
     */
    public List<OrderStateHistory> getOrderHistory(UUID orderId) {
        return getExistingOrder(orderId).getStateHistory();
    }

    private Order getExistingOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void ensureCanAuthorize(Order order) {
        if (!order.canAuthorize()) {
            throw new InvalidOrderStateTransitionException(
                    order.getId(),
                    order.getCurrentState(),
                    "Only INITIALIZED orders can be authorized.");
        }
    }

    private void ensureCanComplete(Order order) {
        if (!order.canComplete()) {
            throw new InvalidOrderStateTransitionException(
                    order.getId(),
                    order.getCurrentState(),
                    "Only PAYMENT_AUTHORIZED orders can be completed.");
        }
    }

    private Instant now() {
        return Instant.now();
    }

    private String deriveLast4(String cardNumber) {
        String digitsOnly = cardNumber.replaceAll("\\s+", "");
        return digitsOnly.substring(digitsOnly.length() - 4);
    }
}
