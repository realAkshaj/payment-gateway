CREATE TABLE IF NOT EXISTS card_tokens (
                                           id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token            VARCHAR(255) NOT NULL UNIQUE,
    encrypted_pan    TEXT NOT NULL,
    iv               VARCHAR(255) NOT NULL,
    last4_digits     VARCHAR(4) NOT NULL,
    card_brand       VARCHAR(20),
    encrypted_expiry TEXT,
    merchant_id      VARCHAR(100) NOT NULL,
    customer_id      VARCHAR(100) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at       TIMESTAMP WITH TIME ZONE NOT NULL
                                   );

CREATE INDEX idx_card_tokens_token ON card_tokens(token);
CREATE INDEX idx_card_tokens_merchant ON card_tokens(merchant_id);