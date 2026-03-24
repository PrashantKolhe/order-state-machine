package com.gametime.challenge.orders.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * Structured API error payload.
 */
@Value
@Builder
public class ErrorResponse {
    String code;
    String message;
    Instant timestamp;
}
