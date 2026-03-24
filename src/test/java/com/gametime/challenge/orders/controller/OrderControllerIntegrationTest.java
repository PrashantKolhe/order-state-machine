package com.gametime.challenge.orders.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void happyPathShouldCreateAuthorizeCompleteAndExposeHistory() throws Exception {
        String orderId = createOrder("happy-path-order");

        mockMvc.perform(post("/orders/{orderId}/authorize", orderId)
                        .contentType(APPLICATION_JSON)
                        .content(validPaymentRequest("4111111111111234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("PAYMENT_AUTHORIZED"))
                .andExpect(jsonPath("$.last4").value("1234"));

        mockMvc.perform(post("/orders/{orderId}/complete", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("COMPLETE"));

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.currentState").value("COMPLETE"));

        mockMvc.perform(get("/orders/{orderId}/history", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(3))
                .andExpect(jsonPath("$.history[0].state").value("INITIALIZED"))
                .andExpect(jsonPath("$.history[1].state").value("PAYMENT_AUTHORIZED"))
                .andExpect(jsonPath("$.history[2].state").value("COMPLETE"));
    }

    @Test
    void paymentDeclinedScenarioShouldRejectOrder() throws Exception {
        String orderId = createOrder("declined-order");

        mockMvc.perform(post("/orders/{orderId}/authorize", orderId)
                        .contentType(APPLICATION_JSON)
                        .content(validPaymentRequest("4111111111111234"))
                        .header("X-Payment-Scenario", "AUTHORIZATION_DECLINED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("REJECTED"))
                .andExpect(jsonPath("$.last4").value("1234"));
    }

    @Test
    void getOrderShouldReturnCurrentOrderStateAndMetadata() throws Exception {
        String orderId = createOrder("get-order-test");

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.eventId").value("get-order-test"))
                .andExpect(jsonPath("$.amountInCents").value(12500))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.currentState").value("INITIALIZED"))
                .andExpect(jsonPath("$.last4").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void completionFailureWithVoidSuccessShouldCancelOrder() throws Exception {
        String orderId = createOrder("cancelled-order");

        mockMvc.perform(post("/orders/{orderId}/authorize", orderId)
                        .contentType(APPLICATION_JSON)
                        .content(validPaymentRequest("4111111111111234")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/orders/{orderId}/complete", orderId)
                        .header("X-Payment-Scenario", "COMPLETION_FAILED_VOID_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("CANCELLED"));
    }

    @Test
    void completionFailureWithVoidFailureShouldMoveOrderToNeedsAttention() throws Exception {
        String orderId = createOrder("needs-attention-order");

        mockMvc.perform(post("/orders/{orderId}/authorize", orderId)
                        .contentType(APPLICATION_JSON)
                        .content(validPaymentRequest("4111111111111234")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/orders/{orderId}/complete", orderId)
                        .header("X-Payment-Scenario", "COMPLETION_FAILED_VOID_FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("NEEDS_ATTENTION"));
    }

    @Test
    void invalidTransitionShouldReturnConflict() throws Exception {
        String orderId = createOrder("invalid-transition-order");

        mockMvc.perform(post("/orders/{orderId}/complete", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only PAYMENT_AUTHORIZED orders can be completed.")));
    }

    @Test
    void invalidScenarioHeaderShouldReturnBadRequest() throws Exception {
        String orderId = createOrder("invalid-scenario-order");

        mockMvc.perform(post("/orders/{orderId}/authorize", orderId)
                        .contentType(APPLICATION_JSON)
                        .content(validPaymentRequest("4111111111111234"))
                        .header("X-Payment-Scenario", "NOT_A_REAL_SCENARIO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_SCENARIO"));
    }

    @Test
    void createOrderShouldValidateRequestBody() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .content("{\"eventId\":\"\",\"amountInCents\":0,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String createOrder(String eventId) throws Exception {
        String responseBody = mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"%s",
                                  "amountInCents":12500,
                                  "currency":"USD"
                                }
                                """.formatted(eventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.amountInCents").value(12500))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.last4").value(org.hamcrest.Matchers.nullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        return response.get("id").asText();
    }

    private String validPaymentRequest(String cardNumber) {
        return """
                {
                  "cardNumber":"%s",
                  "expiryMonth":12,
                  "expiryYear":2030,
                  "cvv":"123"
                }
                """.formatted(cardNumber);
    }
}
