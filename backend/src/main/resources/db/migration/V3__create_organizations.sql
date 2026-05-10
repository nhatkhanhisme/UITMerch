-- V3__create_organizations.sql

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    logo_url        TEXT,
    cover_url       TEXT,
    status          organization_status NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);