package com.gametime.challenge.orders.integration;

import com.gametime.challenge.orders.exception.InvalidPaymentScenarioException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Supported header values that drive the scenario-based payment stub.
 */
public enum PaymentScenario {
    AUTHORIZATION_SUCCESS,
    AUTHORIZATION_DECLINED,
    COMPLETION_FAILED_VOID_SUCCESS,
    COMPLETION_FAILED_VOID_FAILED;

    public static PaymentScenario fromHeader(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return AUTHORIZATION_SUCCESS;
        }

        try {
            return PaymentScenario.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            String supportedValues = Arrays.stream(PaymentScenario.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new InvalidPaymentScenarioException(
                    "Unsupported X-Payment-Scenario value '" + rawValue + "'. Supported values: "
                            + supportedValues + '.');
        }
    }
}
