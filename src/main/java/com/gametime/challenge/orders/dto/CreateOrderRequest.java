package com.gametime.challenge.orders.dto;

import com.gametime.challenge.orders.domain.CurrencyCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload used to create a new order.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "eventId is required.")
    private String eventId;

    @Min(value = 1, message = "amountInCents must be greater than zero.")
    private long amountInCents;

    @NotNull(message = "currency is required.")
    private CurrencyCode currency;
}
