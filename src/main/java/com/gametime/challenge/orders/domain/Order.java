package com.gametime.challenge.orders.domain;

import com.gametime.challenge.orders.exception.InvalidOrderStateTransitionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Aggregate root for the order state machine.
 */
@Getter
public class Order {

    private final UUID id;
    private final String eventId;
    private final long amountInCents;
    private final CurrencyCode currency;
    private String last4;
    private OrderState currentState;
    private final Instant createdTime;
    private Instant modifiedTime;
    private final List<OrderStateHistory> stateHistory = new ArrayList<>();

    public Order(UUID id, String eventId, long amountInCents, CurrencyCode currency, Instant createdTime) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.amountInCents = amountInCents;
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.createdTime = Objects.requireNonNull(createdTime, "createdTime must not be null");
        this.modifiedTime = createdTime;
        this.currentState = OrderState.INITIALIZED;
        this.stateHistory.add(new OrderStateHistory(OrderState.INITIALIZED, createdTime, "Order created."));
    }

    /**
     * Moves the order to the next state and records the transition.
     */
    public void transitionTo(OrderState targetState, Instant timestamp, String reason) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");

        if (!currentState.canTransitionTo(targetState)) {
            throw new InvalidOrderStateTransitionException(
                    id,
                    currentState,
                    "Cannot transition from " + currentState + " to " + targetState + '.');
        }

        this.currentState = targetState;
        this.modifiedTime = timestamp;
        this.stateHistory.add(new OrderStateHistory(targetState, timestamp, reason));
    }

    public boolean canAuthorize() {
        return currentState == OrderState.INITIALIZED;
    }

    public boolean canComplete() {
        return currentState == OrderState.PAYMENT_AUTHORIZED;
    }

    public void updateLast4(String last4) {
        this.last4 = Objects.requireNonNull(last4, "last4 must not be null");
    }

    public List<OrderStateHistory> getStateHistory() {
        return Collections.unmodifiableList(stateHistory);
    }
}
