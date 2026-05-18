-- V27: Pickup schedule — organizer creates a campus pickup slot for confirmed orders

CREATE TABLE pickup_schedules (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id           UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    pickup_date      DATE NOT NULL,
    pickup_time_slot VARCHAR(100) NOT NULL,
    location         VARCHAR(500) NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pickup_schedules_org_id ON pickup_schedules(org_id);

-- Each order can belong to at most one pickup schedule
ALTER TABLE orders
    ADD COLUMN pickup_schedule_id UUID REFERENCES pickup_schedules(id) ON DELETE SET NULL;
