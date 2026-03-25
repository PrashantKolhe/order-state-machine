# Order State Machine Service

## What was built and why
This project is a Spring Boot REST service for an event ticket checkout flow. It models an order state machine with stage-dependent failure recovery.

Core design:

- `Order` acts as the aggregate root for state management and enforces valid transitions through explicit rules in the domain model. 
Each successful transition updates the current state and appends a timestamped entry to the order’s history.
- `OrderService` contains the workflow and recovery logic.
- `OrderRepository` is an in-memory abstraction backed by `ConcurrentHashMap`.
- `PaymentGateway` is a stubbed interface so payment outcomes can be controlled for testing and review.
- `GET /orders/{orderId}` is kept focused on the current order snapshot, while `GET /orders/{orderId}/history` exposes the full transition log separately. 
This keeps the main read response lightweight and makes history retrieval explicit when audit details are needed.

Supported business outcomes:

- `INITIALIZED -> PAYMENT_AUTHORIZED -> COMPLETE`
- payment decline during authorization -> `REJECTED`
- completion failure after authorization + successful void -> `CANCELLED`
- completion failure after authorization + failed void -> `NEEDS_ATTENTION`

## Tech stack
- Java 17
- Spring Boot 3
- Maven Wrapper
- Lombok
- JUnit 5
- Mockito
- MockMvc

## How to run locally
Required installations:

- Java 17
- `curl`
- Bash-compatible shell

The scripts below are intended to handle the local setup flow. In most cases, you should start with `./scripts/setup.sh` instead of installing things manually.

You do not need to install Maven separately because the project includes `./mvnw`.

From a terminal opened in this project folder, run these steps in sequence:

```bash
./scripts/setup.sh
./scripts/start-service.sh
./scripts/run-tests.sh
./scripts/smoke-test.sh
```

What each step does:

- `./scripts/setup.sh`: checks required tools and tries a simple install path on common systems
- `./scripts/start-service.sh`: starts the service on port `8080`
- `./scripts/run-tests.sh`: runs the full test suite
- `./scripts/smoke-test.sh`: creates an order, gets the order, authorizes it, completes it, and fetches history

## API overview
Endpoints:

- `POST /orders`: creates a new order in the `INITIALIZED` state
- `POST /orders/{orderId}/authorize`: authorizes payment and moves the order to `PAYMENT_AUTHORIZED` or `REJECTED`
- `POST /orders/{orderId}/complete`: completes an authorized order or runs failure recovery
- `GET /orders/{orderId}`: returns the current order state and metadata
- `GET /orders/{orderId}/history`: returns chronological state transition history

The full card number is accepted only on `POST /orders/{orderId}/authorize` and is never stored on the order. The service derives and returns only `last4`.

Error responses use this shape:

```json
{
  "code": "INVALID_STATE_TRANSITION",
  "message": "Order ...",
  "timestamp": "2026-03-24T12:00:00Z"
}
```

## Payment scenario header
The payment stub is controlled by the `X-Payment-Scenario` request header.

Supported values:

- `AUTHORIZATION_SUCCESS`
- `AUTHORIZATION_DECLINED`
- `COMPLETION_FAILED_VOID_SUCCESS`
- `COMPLETION_FAILED_VOID_FAILED`

If the header is omitted, the service defaults to `AUTHORIZATION_SUCCESS`.

## Curl examples
Create an order:

```bash
curl -i \
  -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"event-123",
    "amountInCents":12500,
    "currency":"USD"
  }'
```

Get an order:

```bash
curl -i "http://localhost:8080/orders/${ORDER_ID}"
```

Authorize an order:

```bash
curl -i \
  -X POST http://localhost:8080/orders/{orderId}/authorize \
  -H 'Content-Type: application/json' \
  -d '{
    "cardNumber":"4111111111111234",
    "expiryMonth":12,
    "expiryYear":2030,
    "cvv":"123"
  }'
```

Authorize an order with a decline:

```bash
curl -i \
  -X POST http://localhost:8080/orders/{orderId}/authorize \
  -H 'Content-Type: application/json' \
  -H 'X-Payment-Scenario: AUTHORIZATION_DECLINED' \
  -d '{
    "cardNumber":"4111111111111234",
    "expiryMonth":12,
    "expiryYear":2030,
    "cvv":"123"
  }'
```

Complete an order successfully:

```bash
curl -i \
  -X POST http://localhost:8080/orders/{orderId}/complete
```

Complete an order where completion fails and void succeeds:

```bash
curl -i \
  -X POST http://localhost:8080/orders/{orderId}/complete \
  -H 'X-Payment-Scenario: COMPLETION_FAILED_VOID_SUCCESS'
```

Complete an order where completion fails and void also fails:

```bash
curl -i \
  -X POST http://localhost:8080/orders/{orderId}/complete \
  -H 'X-Payment-Scenario: COMPLETION_FAILED_VOID_FAILED'
```

Fetch order history:

```bash
curl -i http://localhost:8080/orders/{orderId}/history
```

Example end-to-end happy path:

```bash
ORDER_ID=$(curl -s \
  -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"event-123",
    "amountInCents":12500,
    "currency":"USD"
  }' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

curl -s "http://localhost:8080/orders/${ORDER_ID}"
curl -s -X POST "http://localhost:8080/orders/${ORDER_ID}/authorize" \
  -H 'Content-Type: application/json' \
  -d '{
    "cardNumber":"4111111111111234",
    "expiryMonth":12,
    "expiryYear":2030,
    "cvv":"123"
  }'
curl -s -X POST "http://localhost:8080/orders/${ORDER_ID}/complete"
curl -s "http://localhost:8080/orders/${ORDER_ID}/history"
```

## Sequence diagram
The reviewer-friendly sequence diagram lives in [docs/order-sequence-diagram.md](/Users/prashantkolhe/Downloads/Order-State-Machine-Java/docs/order-sequence-diagram.md). It matches the current implementation, including:

- create order
- get order
- authorize success and decline
- complete success
- completion failure with void success
- completion failure with void failure


## Testing
The test suite covers:

- happy path: create -> authorize -> complete
- get order: create -> fetch current order state and metadata
- payment decline: create -> authorize declined -> rejected
- completion failure with successful void -> cancelled
- completion failure with failed void -> needs attention
- invalid transitions
- validation and invalid scenario headers

Test layers:

- `OrderServiceTest` for focused service logic coverage
- `OrderControllerIntegrationTest` for end-to-end API verification

Run all tests:

```bash
./scripts/run-tests.sh
```

## Tradeoffs made
- In-memory storage: order data lives only inside the running application, so a restart clears all records.
- Scenario-driven test behavior: `X-Payment-Scenario` is used to simulate downstream outcomes and exercise failure paths end to end.
- Single-process concurrency model: the current design assumes one application instance and does not guard against concurrent updates to the same order.
- Synchronous payment flow: payment actions are handled inline during the API request.
- Manual follow-up after post-capture failure: when completion and void both fail, the order is surfaced as `NEEDS_ATTENTION`.

## How to extend it with more time
- Add persistent storage behind the repository abstraction
- Add optimistic locking or other for concurrent updates
- Split payment authorization, completion, and recovery into explicit downstream integrations
- Add metrics, structured logs, tracing, and health indicators
- To avoid duplicate processing, add idempotency keys for mutation endpoints
- Expand the domain model with more realistic event checkout fields
- Proper authentication for API
