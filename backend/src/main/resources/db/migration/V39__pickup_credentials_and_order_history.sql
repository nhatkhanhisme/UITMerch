CREATE TABLE pickup_tokens (
    order_id UUID PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    pickup_schedule_id UUID REFERENCES pickup_schedules(id) ON DELETE SET NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_pickup_tokens_schedule ON pickup_tokens(pickup_schedule_id);
CREATE TABLE guest_pickup_receipts (
    order_id UUID PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE TABLE order_history (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    source VARCHAR(30) NOT NULL,
    pickup_schedule_id UUID REFERENCES pickup_schedules(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_history_order ON order_history(order_id, created_at, id);
CREATE INDEX idx_order_history_actor ON order_history(actor_id);
CREATE INDEX idx_order_history_schedule ON order_history(pickup_schedule_id);
