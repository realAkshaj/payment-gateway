package com.paymentgateway.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Route definitions for the API Gateway.
 *
 * WHY define routes in Java and not application.yml?
 * Java routes give you full programmatic control —
 * you can add dynamic filters, conditional routing,
 * and custom predicates. YAML routes are fine for
 * simple cases but Java is more powerful.
 *
 * Route anatomy:
 * .route(id, route -> route
 *     .path("/pattern/**")        ← which paths match
 *     .filters(f -> f             ← what to do with them
 *         .stripPrefix(1))        ← remove /api prefix
 *     .uri("http://service:port") ← where to send them
 * )
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Payment Service ───────────────────────────────────────
                // All /api/v1/payments/** requests → payment-service:8081
                .route("payment-service", route -> route
                        .path("/api/v1/payments/**")
                        .uri("http://localhost:8081"))

                // ── Card Vault Service ────────────────────────────────────
                // /api/v1/vault/** → card-vault-service:8082
                // NOTE: In production this would be on a private network
                // not exposed through the public gateway at all
                .route("card-vault-service", route -> route
                        .path("/api/v1/vault/**")
                        .uri("http://localhost:8082"))

                .build();
    }
}