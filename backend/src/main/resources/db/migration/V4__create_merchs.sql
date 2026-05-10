-- V4__create_merchs.sql

CREATE TABLE merch_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           NUMERIC(12, 2) NOT NULL,
    stock           INTEGER NOT NULL DEFAULT 0,
    image_url       TEXT,
    status          merch_item_status NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);