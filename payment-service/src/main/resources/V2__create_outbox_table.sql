-- Outbox table — the Kafka publishing guarantee
CREATE TABLE IF NOT EXISTS outbox_events (
                                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50) NOT NULL,   -- always "payment"
    aggregate_id    UUID NOT NULL,           -- payment ID
    event_type      VARCHAR(100) NOT NULL,   -- "PAYMENT_INITIATED" etc
    payload         JSONB NOT NULL,          -- full event as JSON
    processed       BOOLEAN DEFAULT FALSE,   -- picked up by Debezium?
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at    TIMESTAMP WITH TIME ZONE
                                  );

-- Debezium queries unprocessed events — this index makes it fast
CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed)
    WHERE processed = FALSE;

-- Debezium queries by aggregate for ordered processing
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_id);