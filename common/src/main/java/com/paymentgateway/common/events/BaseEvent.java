package com.paymentgateway.common.events;

import com.paymentgateway.common.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope for every Kafka message in the system.
 *
 * WHY an envelope pattern?
 * Without it, a consumer has to fully deserialize every message
 * to know what type it is. With the envelope, you read eventType
 * first, then decide whether to process or ignore.
 *
 * eventId     → unique ID for this specific event (for deduplication)
 * paymentId   → the payment this event belongs to
 * eventType   → what happened (routes to correct handler)
 * occurredAt  → when it happened (for audit trail)
 * version     → schema version (for future backwards compatibility)
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    private UUID eventId;
    private UUID paymentId;
    private EventType eventType;
    private Instant occurredAt;
    private int version = 1;

    // Called before publishing — ensures every event has
    // a unique ID and timestamp without the caller having to set it
    public void initializeMetadata() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }
}