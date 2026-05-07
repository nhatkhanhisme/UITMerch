-- V1_09__init_collections_table.sql
-- Create collections table for digital collection gallery
-- FR09: Digital Collection - View historical gallery of successful purchases
-- BR09: Only orders with SUCCESS status are added to Collection (enforced in app layer)

CREATE TABLE collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    merch_item_id UUID NOT NULL REFERENCES merch_items(id) ON DELETE RESTRICT,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_customer_item_collection UNIQUE (customer_user_id, merch_item_id)
);

CREATE INDEX idx_collections_customer_user_id ON collections(customer_user_id);
CREATE INDEX idx_collections_merch_item_id ON collections(merch_item_id);
