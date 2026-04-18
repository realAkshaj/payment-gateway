-- Ledger transactions — the header record
CREATE TABLE IF NOT EXISTS ledger_transactions (
                                                   id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id       UUID NOT NULL,
    merchant_id      VARCHAR(100) NOT NULL,
    customer_id      VARCHAR(100) NOT NULL,
    total_amount     DECIMAL(19, 4) NOT NULL,
    currency         VARCHAR(3) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    description      TEXT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Each payment should have at most one PAYMENT transaction
    -- (reversals create separate transactions)
    CONSTRAINT uq_payment_transaction
    UNIQUE (payment_id, transaction_type)
    );

-- Ledger entries — the individual debit/credit lines
CREATE TABLE IF NOT EXISTS ledger_entries (
                                              id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id   UUID NOT NULL REFERENCES ledger_transactions(id),
    payment_id       UUID NOT NULL,
    account_type     VARCHAR(20) NOT NULL,
    account_id       VARCHAR(100) NOT NULL,
    entry_type       VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount           DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(3) NOT NULL,
    description      TEXT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );

-- Indexes for common queries
CREATE INDEX idx_ledger_tx_payment ON ledger_transactions(payment_id);
CREATE INDEX idx_ledger_tx_merchant ON ledger_transactions(merchant_id);
CREATE INDEX idx_ledger_entries_tx ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_payment ON ledger_entries(payment_id);