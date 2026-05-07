-- V1_05__init_carts_table.sql
-- Create shopping carts table for customer merchandise selection
-- BR03: Single-organization constraint (enforced by schema)
-- FR06: Customer can add/remove items from cart

CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    status cart_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT one_active_cart_per_customer_org UNIQUE (customer_user_id, organization_id, status)
);

CREATE INDEX idx_carts_customer_user_id ON carts(customer_user_id);
CREATE INDEX idx_carts_organization_id ON carts(organization_id);
CREATE INDEX idx_carts_status ON carts(status);
