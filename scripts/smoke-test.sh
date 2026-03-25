#!/usr/bin/env bash

set -euo pipefail

BASE_URL="http://localhost:8080"

echo "Creating order..."
create_response="$(curl -sS \
  -X POST "${BASE_URL}/orders" \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"smoke-test-event",
    "amountInCents":12500,
    "currency":"USD"
  }')"

echo "${create_response}"

order_id="$(printf '%s' "${create_response}" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)"

if [[ -z "${order_id}" ]]; then
  echo "Failed to extract order id from create response." >&2
  exit 1
fi

echo
echo "Fetching order ${order_id}..."
curl -sS "${BASE_URL}/orders/${order_id}"
echo

echo
echo "Authorizing order ${order_id}..."
curl -sS \
  -X POST "${BASE_URL}/orders/${order_id}/authorize" \
  -H 'Content-Type: application/json' \
  -d '{
    "cardNumber":"4111111111111234",
    "expiryMonth":12,
    "expiryYear":2030,
    "cvv":"123"
  }'
echo

echo
echo "Completing order ${order_id}..."
curl -sS -X POST "${BASE_URL}/orders/${order_id}/complete"
echo


echo
echo "Fetching history for order ${order_id}..."
curl -sS "${BASE_URL}/orders/${order_id}/history"
echo
