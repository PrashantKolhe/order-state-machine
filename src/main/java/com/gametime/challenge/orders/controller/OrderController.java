package com.gametime.challenge.orders.controller;

import com.gametime.challenge.orders.dto.CreateOrderRequest;
import com.gametime.challenge.orders.dto.OrderHistoryResponse;
import com.gametime.challenge.orders.dto.OrderResponse;
import com.gametime.challenge.orders.dto.PaymentRequest;
import com.gametime.challenge.orders.integration.PaymentScenario;
import com.gametime.challenge.orders.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for creating orders, advancing their state, and querying current state.
 */
@RestController
@Validated
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String PAYMENT_SCENARIO_HEADER = "X-Payment-Scenario";

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @PostMapping("/{orderId}/authorize")
    public ResponseEntity<OrderResponse> authorizeOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest paymentRequest,
            @RequestHeader(value = PAYMENT_SCENARIO_HEADER, required = false) String scenarioHeader) {
        var order = orderService.authorizeOrder(orderId, paymentRequest, PaymentScenario.fromHeader(scenarioHeader));
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<OrderResponse> completeOrder(
            @PathVariable UUID orderId,
            @RequestHeader(value = PAYMENT_SCENARIO_HEADER, required = false) String scenarioHeader) {
        var order = orderService.completeOrder(orderId, PaymentScenario.fromHeader(scenarioHeader));
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(OrderResponse.from(orderService.getOrder(orderId)));
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<OrderHistoryResponse> getOrderHistory(@PathVariable UUID orderId) {
        return ResponseEntity.ok(OrderHistoryResponse.from(orderId, orderService.getOrderHistory(orderId)));
    }
}
