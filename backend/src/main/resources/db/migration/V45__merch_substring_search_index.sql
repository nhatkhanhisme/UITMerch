CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- No partial status predicate: prepared queries also need this index when
-- PostgreSQL chooses a generic plan with a bound status parameter.
CREATE INDEX CONCURRENTLY idx_merch_name_trgm ON merch_items USING gin (lower(name) gin_trgm_ops);
