CREATE SCHEMA app_ops;
REVOKE ALL ON SCHEMA app_ops FROM PUBLIC;
CREATE TABLE app_ops.settings (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    audit_retention_days integer NOT NULL DEFAULT 90 CHECK (audit_retention_days BETWEEN 1 AND 365)
);
INSERT INTO app_ops.settings DEFAULT VALUES;
ALTER TABLE app_ops.settings ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON app_ops.settings FROM PUBLIC;

-- SECURITY INVOKER: only the migration owner/scheduler can invoke these operations.
-- No HTTP request or sleeping application process is involved.
CREATE FUNCTION app_ops.expire_pending_orders() RETURNS integer
LANGUAGE plpgsql SET search_path = pg_catalog AS $$
DECLARE o public.orders%ROWTYPE; item record; m public.merch_items%ROWTYPE;
        event_id uuid; owner_id uuid; total integer := 0; short_id text;
BEGIN
    -- Serialize cron runs while allowing checkout/organizer row locks to proceed.
    IF NOT pg_try_advisory_xact_lock(78219601) THEN RETURN 0; END IF;
    FOR o IN SELECT * FROM public.orders
        WHERE status='PENDING' AND payment_method='CASH_ON_DELIVERY'
          AND pending_expires_at <= now()
          AND NOT EXISTS (SELECT 1 FROM public.campaign_reservations r WHERE r.order_id=orders.id)
        ORDER BY pending_expires_at,id LIMIT 100 FOR UPDATE SKIP LOCKED
    LOOP
        FOR item IN SELECT merch_id,sum(quantity)::integer AS quantity FROM public.order_items
                    WHERE order_id=o.id GROUP BY merch_id ORDER BY merch_id::text LOOP
            SELECT * INTO STRICT m FROM public.merch_items WHERE id=item.merch_id FOR UPDATE;
            UPDATE public.merch_items SET stock=stock+item.quantity,
                restock_cycle=restock_cycle+CASE WHEN stock=0 THEN 1 ELSE 0 END,
                updated_at=now() WHERE id=m.id;
            IF m.stock=0 AND m.status='PUBLISHED' AND EXISTS (
                SELECT 1 FROM public.organizations WHERE id=m.org_id AND status='ACTIVE') THEN
                event_id := gen_random_uuid();
                INSERT INTO public.announcement_events(id,dedupe_key,kind,title,message,org_id,merch_id,created_at)
                VALUES(event_id,'restock:'||m.id||':'||(m.restock_cycle+1),'MERCH_RESTOCKED',
                    'Sản phẩm đã có hàng trở lại',m.name||' đã có hàng trở lại.',m.org_id,m.id,now());
                INSERT INTO public.background_jobs(id,kind,payload)
                    VALUES(gen_random_uuid(),'ANNOUNCEMENT',event_id::text);
            END IF;
        END LOOP;
        UPDATE public.orders SET status='CANCELLED',cancelled_by='system',
            cancel_reason='Pending checkout expired after 48 hours',cancelled_at=now(),updated_at=now()
            WHERE id=o.id;
        INSERT INTO public.order_history(id,order_id,from_status,to_status,source,pickup_schedule_id,created_at)
            VALUES(gen_random_uuid(),o.id,'PENDING','CANCELLED','SYSTEM',o.pickup_schedule_id,now());
        UPDATE public.pickup_tokens SET revoked_at=now() WHERE order_id=o.id;
        short_id := upper(left(o.id::text,8));
        IF o.user_id IS NOT NULL THEN
            INSERT INTO public.notifications(id,user_id,title,message,type,is_read,related_order_id,created_at)
            VALUES(gen_random_uuid(),o.user_id,'Đơn hàng đã bị huỷ','Đơn hàng #'||short_id||' đã bị huỷ.',
                'ORDER_CANCELLED',false,o.id,now());
        END IF;
        SELECT organizations.owner_id INTO STRICT owner_id FROM public.organizations WHERE id=o.org_id;
        INSERT INTO public.notifications(id,user_id,title,message,type,is_read,related_order_id,created_at)
            VALUES(gen_random_uuid(),owner_id,'Đơn hàng bị huỷ','Đơn hàng #'||short_id||' đã hết hạn chờ xác nhận.',
                'ORDER_CANCELLED',false,o.id,now());
        total := total+1;
    END LOOP;
    RETURN total;
END $$;
REVOKE ALL ON FUNCTION app_ops.expire_pending_orders() FROM PUBLIC;

CREATE FUNCTION app_ops.prune_security_audit() RETURNS integer
LANGUAGE plpgsql SET search_path = pg_catalog AS $$
DECLARE removed integer;
BEGIN
    WITH expired AS (
        SELECT id FROM security_audit.events
        WHERE occurred_at < now() - (SELECT audit_retention_days * interval '1 day' FROM app_ops.settings)
        ORDER BY occurred_at,id LIMIT 5000 FOR UPDATE SKIP LOCKED
    ) DELETE FROM security_audit.events e USING expired WHERE e.id=expired.id;
    GET DIAGNOSTICS removed=ROW_COUNT;
    RETURN removed;
END $$;
REVOKE ALL ON FUNCTION app_ops.prune_security_audit() FROM PUBLIC;

-- Historical DEAD jobs retain a null timestamp and require separate triage.
ALTER TABLE public.background_jobs ADD COLUMN failed_at timestamptz;
CREATE FUNCTION app_ops.mark_job_failure() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
    IF NEW.state='DEAD' AND OLD.state IS DISTINCT FROM 'DEAD' THEN NEW.failed_at := now(); END IF;
    RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION app_ops.mark_job_failure() FROM PUBLIC;
CREATE TRIGGER background_job_failure_time BEFORE UPDATE OF state ON public.background_jobs
FOR EACH ROW EXECUTE FUNCTION app_ops.mark_job_failure();

GRANT USAGE ON SCHEMA app_ops TO uitmerch_backup, uitmerch_monitor;
GRANT SELECT ON app_ops.settings TO uitmerch_backup;
CREATE POLICY backup_settings_reader ON app_ops.settings FOR SELECT TO uitmerch_backup USING (true);

-- Only fixed aggregate health data is exposed to the monitor role, with no PII/payloads.
CREATE FUNCTION app_ops.health_report() RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE result jsonb; cron_failed integer := 0; expiry_last timestamptz; retention_last timestamptz;
BEGIN
    IF to_regclass('cron.job_run_details') IS NOT NULL THEN
        EXECUTE $q$SELECT count(*) FROM cron.job_run_details d JOIN cron.job j USING(jobid)
            WHERE j.jobname LIKE 'uitmerch-%' AND d.status='failed' AND d.start_time > now()-interval '1 hour'$q$ INTO cron_failed;
        EXECUTE $q$SELECT max(d.end_time) FROM cron.job_run_details d JOIN cron.job j USING(jobid)
            WHERE j.jobname='uitmerch-cod-expiry' AND d.status='succeeded'$q$ INTO expiry_last;
        EXECUTE $q$SELECT max(d.end_time) FROM cron.job_run_details d JOIN cron.job j USING(jobid)
            WHERE j.jobname='uitmerch-audit-retention' AND d.status='succeeded'$q$ INTO retention_last;
    END IF;
    SELECT jsonb_build_object(
        'overduePendingOrders',count(*) FILTER (WHERE status='PENDING' AND pending_expires_at < now()-interval '5 minutes'),
        'recentDeadJobs',(SELECT count(*) FROM public.background_jobs WHERE state='DEAD' AND failed_at > now()-interval '1 hour'),
        'historicalDeadJobs',(SELECT count(*) FROM public.background_jobs WHERE state='DEAD' AND failed_at IS NULL),
        'stalledJobs',(SELECT count(*) FROM public.background_jobs WHERE state IN ('PENDING','PROCESSING') AND next_attempt_at < now()-interval '1 hour'),
        'cronFailures',cron_failed,'expiryLastSuccess',expiry_last,'retentionLastSuccess',retention_last
    ) INTO result FROM public.orders;
    RETURN result;
END $$;
REVOKE ALL ON FUNCTION app_ops.health_report() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app_ops.health_report() TO uitmerch_monitor;

CREATE FUNCTION app_ops.audit_probe_seen(probe_trace text) RETURNS boolean
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog AS $$
    SELECT EXISTS(SELECT 1 FROM security_audit.events
        WHERE trace_id=probe_trace AND method='POST' AND action='/api/v1/auth/login'
          AND status=401 AND occurred_at > now()-interval '10 minutes')
$$;
REVOKE ALL ON FUNCTION app_ops.audit_probe_seen(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app_ops.audit_probe_seen(text) TO uitmerch_monitor;
