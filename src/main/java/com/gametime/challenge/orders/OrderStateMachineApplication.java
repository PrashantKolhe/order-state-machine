package com.gametime.challenge.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the order state machine service.
 */
@SpringBootApplication
public class OrderStateMachineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderStateMachineApplication.class, args);
    }
}
