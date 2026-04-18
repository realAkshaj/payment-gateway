package com.paymentgateway.payment.repository;

import com.paymentgateway.payment.domain.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Reactive repository for the outbox_events table.
 *
 * This table is the bridge between our database and Kafka.
 * Debezium watches it via the Postgres WAL.
 *
 * We keep this repository intentionally simple —
 * the Outbox is an infrastructure concern, not business logic.
 * The only operations we need are:
 *   1. Save a new event (done inside PaymentService transaction)
 *   2. Find unprocessed events (for the fallback publisher)
 *   3. Mark events as processed
 */
@Repository
public interface OutboxRepository
        extends ReactiveCrudRepository<OutboxEvent, UUID> {

    /**
     * Finds all events not yet picked up by Debezium.
     * Used by the fallback scheduler as a safety net.
     *
     * Spring generates:
     * SELECT * FROM outbox_events WHERE processed = false
     * (uses the partial index we created in V2 migration)
     */
    Flux<OutboxEvent> findByProcessedFalse();
}