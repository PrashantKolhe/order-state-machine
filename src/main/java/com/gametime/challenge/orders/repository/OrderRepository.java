package com.gametime.challenge.orders.repository;

import com.gametime.challenge.orders.domain.Order;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository abstraction for storing and retrieving orders.
 */
public interface OrderRepository {

    /**
     * Saves the current order snapshot.
     */
    Order save(Order order);

    /**
     * Retrieves an order by id.
     */
    Optional<Order> findById(UUID orderId);
}
