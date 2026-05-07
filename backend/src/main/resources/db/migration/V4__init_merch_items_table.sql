-- V1_04__init_merch_items_table.sql
-- Create merchandise items table for organizer inventory management
-- FR05: Organizer can create merch with is_preorder and stock_quantity
-- FR03: Customer can search by keyword, organization
-- BR07: Pre-order items bypass stock_quantity validation (no constraint)
-- BR08: Cannot hard-delete items linked to orders

CREATE TABLE merch_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    name VARCHAR(180) NOT NULL,
    meaning_text TEXT,
    price NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    is_preorder BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT price_non_negative CHECK (price >= 0),
    CONSTRAINT stock_quantity_non_negative CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_merch_items_organization_id ON merch_items(organization_id);
CREATE INDEX idx_merch_items_is_active ON merch_items(is_active);
CREATE INDEX idx_merch_items_is_preorder ON merch_items(is_preorder);
