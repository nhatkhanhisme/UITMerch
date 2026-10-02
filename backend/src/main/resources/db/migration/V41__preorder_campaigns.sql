CREATE TABLE preorder_campaigns (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL REFERENCES organizations(id),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000),
    minimum_quantity INTEGER NOT NULL CHECK (minimum_quantity > 0),
    deadline TIMESTAMPTZ NOT NULL,
    state VARCHAR(16) NOT NULL CHECK (state IN ('ACTIVE','SUCCEEDED','FAILED','CANCELLED')),
    closed_quantity BIGINT,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_campaigns_org_created ON preorder_campaigns(org_id, created_at);
CREATE INDEX idx_campaigns_due ON preorder_campaigns(state, deadline);
CREATE TABLE campaign_variants (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES preorder_campaigns(id),
    merch_id UUID NOT NULL REFERENCES merch_items(id),
    label VARCHAR(128) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    UNIQUE(campaign_id, merch_id)
);
CREATE INDEX idx_campaign_variants_merch ON campaign_variants(merch_id);
CREATE TABLE campaign_reservations (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES preorder_campaigns(id),
    user_id UUID NOT NULL REFERENCES users(id),
    merch_id UUID NOT NULL REFERENCES merch_items(id),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    request_id UUID NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_campaign_reservation_request UNIQUE(user_id, request_id)
);
CREATE INDEX idx_campaign_reservations_campaign ON campaign_reservations(campaign_id);
CREATE INDEX idx_campaign_reservations_merch ON campaign_reservations(merch_id);
