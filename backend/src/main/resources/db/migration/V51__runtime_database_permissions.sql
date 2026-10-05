-- Runtime identity is provisioned with a password outside source control.
-- Migration credentials must never be present in the backend container after cutover.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='uitmerch_runtime') THEN
        CREATE ROLE uitmerch_runtime NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='uitmerch_backup') THEN
        CREATE ROLE uitmerch_backup NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='uitmerch_monitor') THEN
        CREATE ROLE uitmerch_monitor NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
    END IF;
END $$;
REVOKE CREATE ON SCHEMA public, extensions FROM PUBLIC;
GRANT USAGE ON SCHEMA public, extensions, security_audit TO uitmerch_runtime;
DO $$ DECLARE t record; BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename <> 'flyway_schema_history' LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO uitmerch_runtime', t.tablename);
        EXECUTE format('CREATE POLICY backend_runtime ON public.%I TO uitmerch_runtime USING (true) WITH CHECK (true)', t.tablename);
    END LOOP;
END $$;
-- Audit is append-only for the application; deletion belongs to the independent scheduler.
GRANT USAGE ON SCHEMA public, extensions, security_audit TO uitmerch_backup;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO uitmerch_backup;
GRANT SELECT ON security_audit.events TO uitmerch_backup;
DO $$ DECLARE t record; BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
        EXECUTE format('CREATE POLICY backup_reader ON public.%I FOR SELECT TO uitmerch_backup USING (true)', t.tablename);
    END LOOP;
END $$;
CREATE POLICY backup_audit_reader ON security_audit.events FOR SELECT TO uitmerch_backup USING (true);
GRANT INSERT ON security_audit.events TO uitmerch_runtime;
CREATE POLICY runtime_audit_insert ON security_audit.events FOR INSERT TO uitmerch_runtime WITH CHECK (true);
