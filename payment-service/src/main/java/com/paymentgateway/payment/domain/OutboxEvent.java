package com.paymentgateway.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox pattern — the bridge between database and Kafka.
 *
 * HOW IT WORKS:
 * 1. Payment Service writes Payment + OutboxEvent in ONE transaction
 * 2. Debezium watches this table via Postgres WAL (Write-Ahead Log)
 * 3. When Debezium sees a new row, it publishes to Kafka automatically
 * 4. After successful Kafka publish, the row is marked processed
 *
 * WHY NOT just publish directly to Kafka?
 * Because you'd have two separate operations:
 *   a) Write payment to DB
 *   b) Publish to Kafka
 * If the app crashes between (a) and (b), the payment exists
 * but no Kafka event was published. Other services never know
 * the payment happened. Money is stuck in limbo.
 *
 * The Outbox makes (a) and (b) atomic — they either both happen
 * or neither happens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    // Which aggregate this event belongs to (always "payment" here)
    private String aggregateType;

    // The payment ID this event is about
    private UUID aggregateId;

    // e.g. "PAYMENT_INITIATED" — Debezium uses this as the Kafka topic
    private String eventType;

    // Full event payload serialized as JSON
    private String payload;

    // false = not yet picked up by Debezium
    // true = published to Kafka successfully
    private boolean processed;

    private Instant createdAt;
    private Instant processedAt;
}