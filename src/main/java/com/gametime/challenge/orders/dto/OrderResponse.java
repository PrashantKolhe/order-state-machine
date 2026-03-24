package com.gametime.challenge.orders.dto;

import com.gametime.challenge.orders.domain.CurrencyCode;
import com.gametime.challenge.orders.domain.Order;
import com.gametime.challenge.orders.domain.OrderState;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * API representation of an order.
 */
@Value
@Builder
public class OrderResponse {
    UUID id;
    String eventId;
    long amountInCents;
    CurrencyCode currency;
    String last4;
    OrderState currentState;
    Instant createdTime;
    Instant modifiedTime;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .eventId(order.getEventId())
                .amountInCents(order.getAmountInCents())
                .currency(order.getCurrency())
                .last4(order.getLast4())
                .currentState(order.getCurrentState())
                .createdTime(order.getCreatedTime())
                .modifiedTime(order.getModifiedTime())
                .build();
    }
}
