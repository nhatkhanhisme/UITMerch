CREATE TABLE checkout_actors (
    actor_key VARCHAR(80) PRIMARY KEY,
    code_hash VARCHAR(100), challenge_id UUID, code_expires_at TIMESTAMPTZ,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    token_hash VARCHAR(64), verified_until TIMESTAMPTZ
);
CREATE TABLE checkout_attempts (
    id VARCHAR(64) PRIMARY KEY, actor_key VARCHAR(80) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL, response_json TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE orders ADD COLUMN checkout_id VARCHAR(64);
ALTER TABLE orders ADD COLUMN checkout_actor VARCHAR(80);
ALTER TABLE orders ADD COLUMN pending_expires_at TIMESTAMPTZ;
CREATE INDEX idx_order_pending_actor ON orders(checkout_actor, checkout_id) WHERE status='PENDING';
CREATE INDEX idx_order_pending_expiry ON orders(pending_expires_at) WHERE status='PENDING';
CREATE TABLE guest_tracking_credentials (
    order_id UUID PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL, expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE rate_limit_buckets (
    key_hash VARCHAR(64) PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_rate_limit_expiry ON rate_limit_buckets(expires_at);
-- Historical COD orders are deliberately not assigned an expiration automatically.
