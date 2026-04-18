package com.paymentgateway.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — the single entry point for all client requests.
 *
 * Responsibilities:
 * 1. JWT authentication — validate token on every request
 * 2. Rate limiting — per-merchant request throttling via Redis
 * 3. Correlation ID — inject X-Correlation-ID for distributed tracing
 * 4. Routing — forward requests to the correct downstream service
 *
 * Nothing else. No business logic lives here.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}