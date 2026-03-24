package com.gametime.challenge.orders.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payment details supplied during authorization.
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "cardNumber is required.")
    @Pattern(
            regexp = "^[0-9]{12,19}$",
            message = "cardNumber must contain only digits.")
    private String cardNumber;

    @NotNull(message = "expiryMonth is required.")
    @Min(value = 1, message = "expiryMonth must be between 1 and 12.")
    @Max(value = 12, message = "expiryMonth must be between 1 and 12.")
    private Integer expiryMonth;

    @NotNull(message = "expiryYear is required.")
    @Min(value = 2024, message = "expiryYear must be a four digit year.")
    @Max(value = 9999, message = "expiryYear must be a four digit year.")
    private Integer expiryYear;

    @NotBlank(message = "cvv is required.")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "cvv must be 3 or 4 digits.")
    private String cvv;
}
