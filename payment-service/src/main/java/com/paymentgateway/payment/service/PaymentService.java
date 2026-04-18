package com.paymentgateway.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.common.enums.EventType;
import com.paymentgateway.common.enums.PaymentStatus;
import com.paymentgateway.common.events.PaymentInitiatedEvent;
import com.paymentgateway.payment.domain.OutboxEvent;
import com.paymentgateway.payment.domain.Payment;
import com.paymentgateway.payment.dto.PaymentRequest;
import com.paymentgateway.payment.dto.PaymentResponse;
import com.paymentgateway.payment.repository.OutboxRepository;
import com.paymentgateway.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;

/**
 * Core business logic for payment processing.
 *
 * This class has two critical responsibilities:
 *
 * 1. IDEMPOTENCY — never process the same request twice
 *    Every request carries an idempotency_key. Before doing
 *    anything, we check if we've seen this key before.
 *    If yes → return the original response immediately.
 *    If no  → process and store the result.
 *
 * 2. OUTBOX PATTERN — atomic publish guarantee
 *    Payment record + OutboxEvent written in ONE transaction.
 *    Debezium picks up the OutboxEvent and publishes to Kafka.
 *    No dual-write problem. No lost events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new payment with full idempotency guarantee.
     *
     * Flow:
     * 1. Check idempotency key — seen before? return cached response
     * 2. Build Payment entity (status: PENDING)
     * 3. Build OutboxEvent with full payload
     * 4. Save BOTH in one @Transactional operation
     * 5. Return 202 Accepted
     *
     * @param request   the inbound payment request
     * @param correlationId  tracing ID from API Gateway header
     */
    @Transactional
    public Mono<PaymentResponse> initiatePayment(
            PaymentRequest request,
            String correlationId
    ) {
        log.info("Initiating payment — idempotencyKey={} correlationId={}",
                request.getIdempotencyKey(), correlationId);

        // ── Step 1: Idempotency check ─────────────────────────────────────
        // Look up the key BEFORE doing anything else.
        // If found → return the existing payment as-is.
        // This handles retries, network timeouts, duplicate submissions.
        return paymentRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .doOnNext(existing -> log.info(
                        "Duplicate request detected — idempotencyKey={}, " +
                                "returning existing paymentId={}",
                        request.getIdempotencyKey(), existing.getId()))
                .map(this::toResponse)

                // ── Step 2: No existing payment — create a new one ────────
                // switchIfEmpty fires when findByIdempotencyKey returns empty
                .switchIfEmpty(createPaymentWithOutbox(request, correlationId));
    }

    /**
     * The atomic write — payment + outbox in one transaction.
     *
     * WHY @Transactional on the parent method and not here?
     * Spring's @Transactional works by wrapping the method in a
     * proxy. Since createPaymentWithOutbox is called internally
     * (not through the proxy), the transaction from initiatePayment
     * covers both saves automatically.
     *
     * Both saves succeed → transaction commits → Debezium picks up outbox
     * Either save fails → transaction rolls back → nothing is saved
     */
    private Mono<PaymentResponse> createPaymentWithOutbox(
            PaymentRequest request,
            String correlationId
    ) {
        // Build the payment entity
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchantId(request.getMerchantId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .cardToken(request.getCardToken())
                .idempotencyKey(request.getIdempotencyKey())
                .correlationId(correlationId)
                .build();

        // Save payment first, then save outbox event
        // Both happen inside the same @Transactional boundary
        return paymentRepository.save(payment)
                .flatMap(savedPayment -> {
                    // Build the Kafka event payload
                    OutboxEvent outboxEvent = buildOutboxEvent(savedPayment);

                    // Save outbox event — same transaction as payment save
                    return outboxRepository.save(outboxEvent)
                            .map(savedOutbox -> {
                                log.info("Payment + OutboxEvent saved atomically" +
                                        " — paymentId={}", savedPayment.getId());
                                return toResponse(savedPayment);
                            });
                })
                // If we get a duplicate key violation on idempotency_key
                // (two identical requests arrived at exactly the same time),
                // look up and return the existing payment
                .onErrorResume(ex -> isUniqueConstraintViolation(ex)
                        ? paymentRepository
                        .findByIdempotencyKey(request.getIdempotencyKey())
                        .map(this::toResponse)
                        : Mono.error(ex));
    }

    /**
     * Builds the OutboxEvent that Debezium will pick up.
     *
     * WHY serialize the full event as JSON payload?
     * Debezium publishes the payload as-is to Kafka.
     * Downstream services (Card Vault, Ledger) deserialize
     * this JSON back into a PaymentInitiatedEvent object.
     * The full event must be self-contained — no DB lookups
     * required by consumers.
     */
    private OutboxEvent buildOutboxEvent(Payment payment) {
        // Build the Kafka event
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .eventId(UUID.randomUUID())
                .paymentId(payment.getId())
                .eventType(EventType.PAYMENT_INITIATED)
                .occurredAt(Instant.now())
                .version(1)
                .merchantId(payment.getMerchantId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .cardToken(payment.getCardToken())
                .idempotencyKey(payment.getIdempotencyKey())
                .build();

        // Serialize to JSON for storage in outbox
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payment event", e);
        }

        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("payment")
                .aggregateId(payment.getId())
                .eventType(EventType.PAYMENT_INITIATED.name())
                .payload(Json.of(payload))
                .processed(false)
                .build();
    }

    /**
     * Maps internal Payment entity to the public PaymentResponse DTO.
     *
     * WHY a separate mapping method?
     * Single responsibility — one place to control what we expose.
     * If we add a sensitive field to Payment later, it won't
     * accidentally leak into the response.
     */
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .merchantId(payment.getMerchantId())
                .idempotencyKey(payment.getIdempotencyKey())
                .createdAt(payment.getCreatedAt())
                .failureReason(payment.getFailureReason())
                .build();
    }

    /**
     * Detects PostgreSQL unique constraint violations.
     * Used to handle the race condition where two identical
     * requests arrive simultaneously and both pass the
     * idempotency check before either one saves.
     *
     * The DB unique constraint on idempotency_key is the
     * FINAL safety net — even if our application-level check
     * misses a duplicate, Postgres will catch it here.
     */
    private boolean isUniqueConstraintViolation(Throwable ex) {
        return ex.getMessage() != null &&
                ex.getMessage().contains("uq_idempotency_key");
    }

    /**
     * Retrieves a payment by ID for status polling.
     * Returns empty Mono if not found — controller returns 404.
     */
    public Mono<PaymentResponse> getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::toResponse);
    }

}