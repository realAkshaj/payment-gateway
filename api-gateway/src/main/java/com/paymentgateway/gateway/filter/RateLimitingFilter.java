package com.paymentgateway.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Per-merchant rate limiting using Redis token bucket.
 *
 * WHY rate limit at the gateway?
 * Without rate limiting, a single merchant with a buggy
 * integration could flood the system with thousands of
 * requests per second, starving other merchants.
 *
 * The limit is per-merchant, not per-IP.
 * WHY per-merchant?
 * A merchant might have multiple servers all hitting the
 * gateway. Per-IP would unfairly limit them. Per-merchant
 * is the right business boundary.
 *
 * Algorithm: fixed window counter
 * Simple, predictable, Redis-friendly.
 * In production you'd use sliding window for smoother limits.
 */
@Slf4j
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // 100 requests per 10 seconds per merchant
    private static final long MAX_REQUESTS = 100;
    private static final Duration WINDOW = Duration.ofSeconds(10);

    public RateLimitingFilter(
            ReactiveRedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        // Get merchantId injected by JwtAuthenticationFilter
        String merchantId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Merchant-ID");

        // No merchantId = public path, skip rate limiting
        if (merchantId == null) {
            return chain.filter(exchange);
        }

        String redisKey = "rate_limit:" + merchantId;

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request in window — set expiry
                        return redisTemplate.expire(redisKey, WINDOW)
                                .then(Mono.just(count));
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > MAX_REQUESTS) {
                        log.warn("Rate limit exceeded — merchantId={} count={}",
                                merchantId, count);
                        exchange.getResponse()
                                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse()
                                .getHeaders()
                                .add("X-RateLimit-Limit",
                                        String.valueOf(MAX_REQUESTS));
                        exchange.getResponse()
                                .getHeaders()
                                .add("X-RateLimit-Remaining", "0");
                        return exchange.getResponse().setComplete();
                    }

                    // Add rate limit headers to response
                    exchange.getResponse()
                            .getHeaders()
                            .add("X-RateLimit-Remaining",
                                    String.valueOf(MAX_REQUESTS - count));
                    return chain.filter(exchange);
                });
    }

    // Runs after JWT filter (order 0 > order -1)
    @Override
    public int getOrder() {
        return 0;
    }
}