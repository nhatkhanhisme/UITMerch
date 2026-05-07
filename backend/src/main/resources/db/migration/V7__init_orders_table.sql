-- V1_07__init_orders_table.sql
-- Create orders table for customer checkout and payment tracking
-- FR07: COD checkout creates PENDING order with idempotency support
-- BR04: Idempotency-Key must be unique per order
-- BR05: Payment method is exclusively COD in MVP
-- BR06: Order status lifecycle: PENDING -> CONFIRMED -> READY_FOR_PICKUP -> SUCCESS or CANCELLED
-- BR08: Cannot hard-delete orders

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code VARCHAR(30) NOT NULL UNIQUE,
    customer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    status order_status NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(12, 2) NOT NULL,
    payment_method payment_method NOT NULL DEFAULT 'CASH_ON_DELIVERY',
    placed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT order_code_not_empty CHECK (LENGTH(TRIM(order_code)) > 0),
    CONSTRAINT idempotency_key_not_empty CHECK (LENGTH(TRIM(idempotency_key)) > 0),
    CONSTRAINT total_amount_non_negative CHECK (total_amount >= 0),
    CONSTRAINT payment_method_cod CHECK (payment_method = 'CASH_ON_DELIVERY')
);

CREATE INDEX idx_orders_customer_user_id ON orders(customer_user_id);
CREATE INDEX idx_orders_organization_id ON orders(organization_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_code ON orders(order_code);
CREATE INDEX idx_orders_idempotency_key ON orders(idempotency_key);
CREATE INDEX idx_orders_placed_at ON orders(placed_at);
