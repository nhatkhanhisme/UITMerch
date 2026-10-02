CREATE TABLE organization_follows (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notify_merch BOOLEAN NOT NULL DEFAULT TRUE,
    notify_events BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    followed_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, org_id)
);
CREATE INDEX idx_follow_audience ON organization_follows(org_id, user_id) WHERE enabled;
ALTER TABLE merch_items ADD COLUMN publication_announced BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE events ADD COLUMN publication_announced BOOLEAN NOT NULL DEFAULT FALSE;
-- Existing publications must not generate a new-collection alert on their next edit.
UPDATE merch_items SET publication_announced = TRUE WHERE status = 'PUBLISHED';
UPDATE events SET publication_announced = TRUE WHERE status <> 'DRAFT';
