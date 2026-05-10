-- V5__create_events.sql

CREATE TABLE events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    cover_url       TEXT,
    status          event_status NOT NULL DEFAULT 'DRAFT',
    starts_at       TIMESTAMP,
    ends_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Join table: merch thuộc về event
CREATE TABLE event_merch (
    event_id        UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    merch_id        UUID NOT NULL REFERENCES merch_items(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, merch_id)
);