-- Not exposed by the Data API. Backend database owner writes and administers retention.
CREATE SCHEMA security_audit;
REVOKE ALL ON SCHEMA security_audit FROM PUBLIC;
CREATE TABLE security_audit.events (
    id uuid PRIMARY KEY,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    method varchar(8) NOT NULL,
    action varchar(240) NOT NULL,
    actor_id uuid,
    status smallint NOT NULL CHECK (status BETWEEN 100 AND 599),
    trace_id varchar(64)
);
CREATE INDEX security_audit_events_retention ON security_audit.events (occurred_at, id);
ALTER TABLE security_audit.events ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON security_audit.events FROM PUBLIC;
DO $$ DECLARE r text; BEGIN
    FOREACH r IN ARRAY ARRAY['anon','authenticated','service_role'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname=r) THEN
            EXECUTE format('REVOKE ALL ON SCHEMA security_audit FROM %I', r);
            EXECUTE format('REVOKE ALL ON security_audit.events FROM %I', r);
        END IF;
    END LOOP;
END $$;
