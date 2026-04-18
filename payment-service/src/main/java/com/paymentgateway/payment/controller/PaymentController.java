package com.paymentgateway.payment.controller;

import com.paymentgateway.payment.dto.PaymentRequest;
import com.paymentgateway.payment.dto.PaymentResponse;
import com.paymentgateway.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * REST controller for payment operations.
 *
 * WHY only one endpoint right now?
 * We follow the principle of building incrementally.
 * POST /payments is the critical path — everything else
 * (GET status, POST refund) builds on top of it.
 *
 * WHY Mono<ResponseEntity<PaymentResponse>>?
 * Mono       = reactive wrapper (non-blocking)
 * ResponseEntity = lets us control the HTTP status code
 * PaymentResponse = our outbound DTO
 *
 * We return 202 ACCEPTED not 201 CREATED because the payment
 * isn't fully processed when we return — the SAGA is still
 * running. 202 means "we got it, we're working on it."
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiates a new payment.
     *
     * The flow:
     * 1. @Valid runs all annotations on PaymentRequest
     *    → invalid request? Spring returns 400 automatically
     * 2. Extract X-Correlation-ID from header
     *    → if missing, generate a new one
     * 3. Call PaymentService.initiatePayment()
     *    → idempotency check + atomic DB write
     * 4. Return 202 Accepted with PaymentResponse body
     *
     * @param request   validated payment request body
     * @param exchange  the full HTTP exchange (for headers)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ResponseEntity<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            ServerWebExchange exchange
    ) {
        // Extract correlation ID from header
        // API Gateway sets this — if missing we generate one
        // This ID will appear in every log line across all services
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Correlation-ID");

        if (correlationId == null) {
            correlationId = java.util.UUID.randomUUID().toString();
            log.warn("No X-Correlation-ID header found, generated: {}",
                    correlationId);
        }

        // Store correlationId in a final variable for use in lambda
        final String finalCorrelationId = correlationId;

        log.info("Received payment request — merchantId={} idempotencyKey={} " +
                        "correlationId={}",
                request.getMerchantId(),
                request.getIdempotencyKey(),
                finalCorrelationId);

        return paymentService
                .initiatePayment(request, finalCorrelationId)
                .map(response -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body(response))
                .doOnSuccess(response -> log.info(
                        "Payment accepted — paymentId={} correlationId={}",
                        response.getBody().getPaymentId(),
                        finalCorrelationId));
    }

    /**
     * Retrieves payment status by ID.
     *
     * WHY do we need this?
     * After POST /payments returns 202, the client needs a way
     * to check if the payment was eventually AUTHORIZED or FAILED.
     * This is the polling endpoint for that.
     *
     * In a production system you'd also push status updates
     * via webhooks — but polling is the fallback.
     */
    @GetMapping("/{paymentId}")
    public Mono<ResponseEntity<PaymentResponse>> getPayment(
            @PathVariable java.util.UUID paymentId
    ) {
        log.info("Fetching payment status — paymentId={}", paymentId);

        return paymentService
                .getPayment(paymentId)
                .map(response -> ResponseEntity.ok(response))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}