package com.gametime.challenge.orders.dto;

import com.gametime.challenge.orders.domain.OrderStateHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * API representation of an order's state history.
 */
@Value
@Builder
public class OrderHistoryResponse {
    UUID orderId;
    List<OrderStateHistoryResponse> history;

    public static OrderHistoryResponse from(UUID orderId, List<OrderStateHistory> history) {
        List<OrderStateHistoryResponse> historyResponses = new ArrayList<OrderStateHistoryResponse>();
        for (OrderStateHistory stateHistory : history) {
            historyResponses.add(OrderStateHistoryResponse.from(stateHistory));
        }

        return OrderHistoryResponse.builder()
                .orderId(orderId)
                .history(historyResponses)
                .build();
    }
}
