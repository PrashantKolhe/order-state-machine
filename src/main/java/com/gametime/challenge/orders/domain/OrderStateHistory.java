package com.gametime.challenge.orders.domain;

import java.time.Instant;
import lombok.Value;

/**
 * Captures a timestamped snapshot of a state transition.
 */
@Value
public class OrderStateHistory {
    OrderState state;
    Instant timestamp;
    String reason;
}
