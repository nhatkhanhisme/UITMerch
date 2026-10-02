ALTER TABLE merch_items ADD COLUMN restock_cycle BIGINT NOT NULL DEFAULT 0 CHECK (restock_cycle >= 0);

CREATE TABLE restock_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    merch_id UUID NOT NULL REFERENCES merch_items(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, merch_id)
);
CREATE INDEX idx_restock_audience ON restock_subscriptions(merch_id, user_id) WHERE enabled;

CREATE TABLE announcement_events (
    id UUID PRIMARY KEY,
    dedupe_key VARCHAR(200) NOT NULL UNIQUE,
    kind VARCHAR(60) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    merch_id UUID REFERENCES merch_items(id) ON DELETE CASCADE,
    event_id UUID REFERENCES events(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    cursor_user_id UUID,
    finished BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_announcements_org ON announcement_events(org_id);
CREATE INDEX idx_announcements_merch ON announcement_events(merch_id);
CREATE INDEX idx_announcements_event ON announcement_events(event_id);

ALTER TABLE notifications ADD COLUMN related_merch_id UUID REFERENCES merch_items(id) ON DELETE SET NULL;
ALTER TABLE notifications ADD COLUMN related_org_id UUID REFERENCES organizations(id) ON DELETE SET NULL;
ALTER TABLE notifications ADD COLUMN related_event_id UUID REFERENCES events(id) ON DELETE SET NULL;
ALTER TABLE notifications ADD COLUMN delivery_event_id UUID REFERENCES announcement_events(id) ON DELETE SET NULL;
ALTER TABLE notifications ADD CONSTRAINT uq_notification_delivery UNIQUE (delivery_event_id, user_id);
CREATE INDEX idx_notifications_merch ON notifications(related_merch_id);
CREATE INDEX idx_notifications_org ON notifications(related_org_id);
CREATE INDEX idx_notifications_event ON notifications(related_event_id);

ALTER TABLE background_jobs DROP CONSTRAINT background_jobs_kind_check;
ALTER TABLE background_jobs ADD CONSTRAINT background_jobs_kind_check CHECK (kind IN ('EMAIL', 'EMBEDDING', 'ANNOUNCEMENT'));
