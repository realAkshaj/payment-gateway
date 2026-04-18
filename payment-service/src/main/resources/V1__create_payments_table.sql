-- Payment table — core domain entity
CREATE TABLE IF NOT EXISTS payments (
                                        id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id      VARCHAR(100) NOT NULL,
    customer_id      VARCHAR(100) NOT NULL,
    amount           DECIMAL(19, 4) NOT NULL,  -- 19 digits, 4 decimal places
    currency         VARCHAR(3) NOT NULL,        -- ISO 4217
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method   VARCHAR(20) NOT NULL,
    card_token       VARCHAR(255),               -- null for NET_BANKING
    idempotency_key  VARCHAR(255) NOT NULL,
    correlation_id   VARCHAR(255),
    failure_reason   TEXT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- This is the idempotency guarantee at the database level.
    -- Even if two requests arrive simultaneously with the same key,
    -- Postgres will only allow one row. The second gets a unique
    -- constraint violation — which we catch and return the original response.
    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key)
    );

-- Index for merchant lookups (settlement service queries by merchant)
CREATE INDEX idx_payments_merchant_id ON payments(merchant_id);

-- Index for status queries (find all CAPTURED payments for settlement)
CREATE INDEX idx_payments_status ON payments(status);

-- Index for customer lookups
CREATE INDEX idx_payments_customer_id ON payments(customer_id);