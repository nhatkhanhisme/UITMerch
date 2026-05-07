-- V1_03__init_organizations_table.sql
-- Create organizations table for organizer-managed merchandise collections
-- FR04: Organizer can update org profile
-- BR08: Cannot hard-delete orgs linked to orders

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    logo_url TEXT,
    status organization_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_organizations_owner_user_id ON organizations(owner_user_id);
CREATE INDEX idx_organizations_status ON organizations(status);
