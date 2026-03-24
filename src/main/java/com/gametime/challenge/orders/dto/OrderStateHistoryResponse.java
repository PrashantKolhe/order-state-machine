package com.gametime.challenge.orders.dto;

import com.gametime.challenge.orders.domain.OrderState;
import com.gametime.challenge.orders.domain.OrderStateHistory;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * API representation of a single state history entry.
 */
@Value
@Builder
public class OrderStateHistoryResponse {
    OrderState state;
    Instant timestamp;
    String reason;

    public static OrderStateHistoryResponse from(OrderStateHistory history) {
        return OrderStateHistoryResponse.builder()
                .state(history.getState())
                .timestamp(history.getTimestamp())
                .reason(history.getReason())
                .build();
    }
}
