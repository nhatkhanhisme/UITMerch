-- Run once with migration credentials after V52; not part of portable Flyway migrations.
CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA pg_catalog;
SELECT cron.schedule('uitmerch-cod-expiry','* * * * *','SELECT app_ops.expire_pending_orders()');
SELECT cron.schedule('uitmerch-audit-retention','17 * * * *','SELECT app_ops.prune_security_audit()');
SELECT cron.schedule('uitmerch-cron-history-retention','43 3 * * *',
    $$DELETE FROM cron.job_run_details WHERE end_time < now() - interval '30 days'$$);
