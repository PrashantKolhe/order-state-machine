package com.gametime.challenge.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gametime.challenge.orders.domain.CurrencyCode;
import com.gametime.challenge.orders.domain.Order;
import com.gametime.challenge.orders.domain.OrderState;
import com.gametime.challenge.orders.dto.CreateOrderRequest;
import com.gametime.challenge.orders.dto.PaymentRequest;
import com.gametime.challenge.orders.exception.InvalidOrderStateTransitionException;
import com.gametime.challenge.orders.exception.OrderNotFoundException;
import com.gametime.challenge.orders.integration.PaymentAuthorizationResult;
import com.gametime.challenge.orders.integration.PaymentCompletionResult;
import com.gametime.challenge.orders.integration.PaymentGateway;
import com.gametime.challenge.orders.integration.PaymentScenario;
import com.gametime.challenge.orders.integration.PaymentVoidResult;
import com.gametime.challenge.orders.repository.OrderRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, paymentGateway);
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createOrderShouldInitializeStateAndHistory() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setEventId(" event-123 ");
        request.setAmountInCents(12500);
        request.setCurrency(CurrencyCode.USD);
        Instant beforeCreate = Instant.now();

        Order order = orderService.createOrder(request);
        Instant afterCreate = Instant.now();

        assertThat(order.getId()).isNotNull();
        assertThat(order.getEventId()).isEqualTo("event-123");
        assertThat(order.getAmountInCents()).isEqualTo(12500);
        assertThat(order.getCurrency()).isEqualTo(CurrencyCode.USD);
        assertThat(order.getLast4()).isNull();
        assertThat(order.getCurrentState()).isEqualTo(OrderState.INITIALIZED);
        assertThat(order.getCreatedTime()).isBetween(beforeCreate, afterCreate);
        assertThat(order.getModifiedTime()).isEqualTo(order.getCreatedTime());
        assertThat(order.getStateHistory()).hasSize(1);
        assertThat(order.getStateHistory().get(0).getState()).isEqualTo(OrderState.INITIALIZED);
    }

    @Test
    void authorizeOrderShouldMoveToAuthorizedWhenGatewayApproves() {
        Order order = buildOrder();
        PaymentRequest paymentRequest = buildPaymentRequest("4111111111111234");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentGateway.authorizePayment(order, PaymentScenario.AUTHORIZATION_SUCCESS))
                .thenReturn(new PaymentAuthorizationResult(
                        PaymentAuthorizationResult.AuthorizationStatus.APPROVED,
                        "Payment authorization succeeded."));

        Order updatedOrder = orderService.authorizeOrder(
                order.getId(),
                paymentRequest,
                PaymentScenario.AUTHORIZATION_SUCCESS);

        assertThat(updatedOrder.getCurrentState()).isEqualTo(OrderState.PAYMENT_AUTHORIZED);
        assertThat(updatedOrder.getLast4()).isEqualTo("1234");
        assertThat(updatedOrder.getStateHistory()).hasSize(2);
        assertThat(updatedOrder.getStateHistory().get(1).getReason())
                .isEqualTo("Payment authorization succeeded.");
    }

    @Test
    void authorizeOrderShouldMoveToRejectedWhenGatewayDeclines() {
        Order order = buildOrder();
        PaymentRequest paymentRequest = buildPaymentRequest("4111111111114321");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentGateway.authorizePayment(order, PaymentScenario.AUTHORIZATION_DECLINED))
                .thenReturn(new PaymentAuthorizationResult(
                        PaymentAuthorizationResult.AuthorizationStatus.DECLINED,
                        "Payment authorization was declined."));

        Order updatedOrder = orderService.authorizeOrder(
                order.getId(),
                paymentRequest,
                PaymentScenario.AUTHORIZATION_DECLINED);

        assertThat(updatedOrder.getCurrentState()).isEqualTo(OrderState.REJECTED);
        assertThat(updatedOrder.getLast4()).isEqualTo("4321");
        assertThat(updatedOrder.getStateHistory()).hasSize(2);
        assertThat(updatedOrder.getStateHistory().get(1).getReason())
                .contains("declined");
    }

    @Test
    void completeOrderShouldMoveToCompletedWhenCompletionSucceeds() {
        Order order = buildOrder();
        order.transitionTo(OrderState.PAYMENT_AUTHORIZED, Instant.now(), "Payment authorization succeeded.");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentGateway.completeAuthorizedOrder(order, PaymentScenario.AUTHORIZATION_SUCCESS))
                .thenReturn(new PaymentCompletionResult(
                        PaymentCompletionResult.CompletionStatus.SUCCEEDED,
                        "Order completed successfully."));

        Order updatedOrder = orderService.completeOrder(order.getId(), PaymentScenario.AUTHORIZATION_SUCCESS);

        assertThat(updatedOrder.getCurrentState()).isEqualTo(OrderState.COMPLETE);
        assertThat(updatedOrder.getStateHistory()).hasSize(3);
    }

    @Test
    void completeOrderShouldCancelWhenCompletionFailsAndVoidSucceeds() {
        Order order = buildOrder();
        order.transitionTo(OrderState.PAYMENT_AUTHORIZED, Instant.now(), "Payment authorization succeeded.");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentGateway.completeAuthorizedOrder(order, PaymentScenario.COMPLETION_FAILED_VOID_SUCCESS))
                .thenReturn(new PaymentCompletionResult(
                        PaymentCompletionResult.CompletionStatus.FAILED,
                        "Order completion failed after payment authorization."));
        when(paymentGateway.voidPayment(order, PaymentScenario.COMPLETION_FAILED_VOID_SUCCESS))
                .thenReturn(new PaymentVoidResult(
                        PaymentVoidResult.VoidStatus.SUCCEEDED,
                        "Authorized payment was voided successfully."));

        Order updatedOrder = orderService.completeOrder(order.getId(), PaymentScenario.COMPLETION_FAILED_VOID_SUCCESS);

        assertThat(updatedOrder.getCurrentState()).isEqualTo(OrderState.CANCELLED);
        assertThat(updatedOrder.getStateHistory().get(2).getReason()).contains("voided successfully");
    }

    @Test
    void completeOrderShouldMoveToNeedsAttentionWhenCompletionAndVoidFail() {
        Order order = buildOrder();
        order.transitionTo(OrderState.PAYMENT_AUTHORIZED, Instant.now(), "Payment authorization succeeded.");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentGateway.completeAuthorizedOrder(order, PaymentScenario.COMPLETION_FAILED_VOID_FAILED))
                .thenReturn(new PaymentCompletionResult(
                        PaymentCompletionResult.CompletionStatus.FAILED,
                        "Order completion failed after payment authorization."));
        when(paymentGateway.voidPayment(order, PaymentScenario.COMPLETION_FAILED_VOID_FAILED))
                .thenReturn(new PaymentVoidResult(
                        PaymentVoidResult.VoidStatus.FAILED,
                        "Payment void failed; manual follow-up is required."));

        Order updatedOrder = orderService.completeOrder(order.getId(), PaymentScenario.COMPLETION_FAILED_VOID_FAILED);

        assertThat(updatedOrder.getCurrentState()).isEqualTo(OrderState.NEEDS_ATTENTION);
        assertThat(updatedOrder.getStateHistory().get(2).getReason()).contains("manual follow-up");
    }

    @Test
    void completeOrderShouldRejectInvalidTransition() {
        Order order = buildOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeOrder(order.getId(), PaymentScenario.AUTHORIZATION_SUCCESS))
                .isInstanceOf(InvalidOrderStateTransitionException.class)
                .hasMessageContaining("Only PAYMENT_AUTHORIZED orders can be completed.");

        verify(paymentGateway, never()).completeAuthorizedOrder(any(), any());
        verify(paymentGateway, never()).voidPayment(any(), any());
    }

    @Test
    void getOrderShouldThrowWhenOrderDoesNotExist() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    private Order buildOrder() {
        return new Order(
                UUID.randomUUID(),
                "event-123",
                12500,
                CurrencyCode.USD,
                Instant.now());
    }

    private PaymentRequest buildPaymentRequest(String cardNumber) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setCardNumber(cardNumber);
        paymentRequest.setExpiryMonth(12);
        paymentRequest.setExpiryYear(2030);
        paymentRequest.setCvv("123");
        return paymentRequest;
    }
}
