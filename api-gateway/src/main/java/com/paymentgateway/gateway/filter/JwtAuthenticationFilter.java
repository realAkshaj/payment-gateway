package com.paymentgateway.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Global JWT authentication filter.
 *
 * Runs on EVERY request before routing.
 * Order = -1 means it runs before all other filters.
 *
 * Flow:
 * 1. Check if path is public (actuator, health) → skip auth
 * 2. Extract Authorization header
 * 3. Validate JWT signature and expiry
 * 4. Extract merchantId from claims
 * 5. Inject merchantId + correlationId as headers
 * 6. Forward to downstream service
 *
 * WHY inject merchantId as a header?
 * Downstream services (Payment Service) need to know which
 * merchant made the request. Rather than making each service
 * parse the JWT themselves, the gateway does it once and
 * passes the result as a trusted internal header.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    // In production this would be RSA public key, not a shared secret
    // For local dev, a shared secret is fine
    @Value("${jwt.secret:mySecretKeyForDevelopmentOnlyNotForProduction123}")
    private String jwtSecret;

    // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator",
            "/actuator/health",
            "/api/v1/auth"
    );

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        String path = exchange.getRequest().getPath().value();

        // Skip auth for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header — path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7); // Remove "Bearer "

        try {
            // Validate and parse JWT
            Claims claims = validateToken(token);

            String merchantId = claims.get("merchantId", String.class);
            String subject = claims.getSubject();

            // Generate correlation ID for distributed tracing
            String correlationId = UUID.randomUUID().toString();

            log.info("Authenticated request — merchantId={} path={} correlationId={}",
                    merchantId, path, correlationId);

            // Inject headers for downstream services
            // These are INTERNAL headers — not from the client
            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-Merchant-ID", merchantId)
                    .header("X-User-ID", subject)
                    .header("X-Correlation-ID", correlationId)
                    .build();

            return chain.filter(
                    exchange.mutate()
                            .request(mutatedRequest)
                            .build()
            );

        } catch (Exception e) {
            log.warn("JWT validation failed — path={} error={}",
                    path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Validates JWT signature and expiry.
     * Throws exception if invalid — caught by the filter.
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(path::startsWith);
    }

    // Runs before other filters
    @Override
    public int getOrder() {
        return -1;
    }
}