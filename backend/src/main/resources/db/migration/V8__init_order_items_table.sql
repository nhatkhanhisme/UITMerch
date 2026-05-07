-- V1_08__init_order_items_table.sql
-- Create order items table for line items within orders
-- Snapshot of merch items at time of order

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    merch_item_id UUID NOT NULL REFERENCES merch_items(id) ON DELETE RESTRICT,
    unit_price_snapshot NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unit_price_snapshot_non_negative CHECK (unit_price_snapshot >= 0),
    CONSTRAINT quantity_positive CHECK (quantity > 0),
    CONSTRAINT unique_item_per_order UNIQUE (order_id, merch_item_id)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_merch_item_id ON order_items(merch_item_id);
