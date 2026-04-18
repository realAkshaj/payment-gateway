package com.paymentgateway.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Service — SAGA orchestrator and idempotency enforcer.
 *
 * This service owns the payment lifecycle:
 * PENDING → PROCESSING → AUTHORIZED → CAPTURED → SETTLED/FAILED
 *
 * Key responsibilities:
 * 1. Accept payment requests and enforce idempotency
 * 2. Write payment + outbox record atomically (no dual-write)
 * 3. Orchestrate the SAGA via Kafka events
 * 4. Handle compensating transactions on failure
 */
@SpringBootApplication
@EnableScheduling
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}