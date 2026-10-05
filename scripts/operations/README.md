# Production operations

Source changes use the protected main branch and all three CI gates. Deploy and operations jobs use the `production` GitHub environment, limited to protected branches. Migration credentials live only in that environment; the backend uses `uitmerch_runtime`, with no DDL, role management, RLS bypass, audit read/delete or migration-history access. Backup is read-only; monitoring can execute only two fixed aggregate functions.

After applying V51/V52, provision passwords for the three NOLOGIN roles outside Git. Enable LOGIN without changing their permission flags. Use the Supabase **session pooler on port 5432**, with users `uitmerch_runtime.PROJECT_REF`, `uitmerch_backup.PROJECT_REF` and `uitmerch_monitor.PROJECT_REF`. Never put migration credentials or passwords in a JDBC URL on Render. Disable Flyway in runtime and clear its old URL/user/password environment values.

Run `install-cron.sql` with migration credentials once on Supabase. COD expiry runs every minute; audit retention runs hourly with 90 days configured in `app_ops.settings`. SQL locks orders, restores stock, records history, revokes pickup tokens and saves customer/organizer notifications atomically. Restock announcements enter the durable queue. Confirmed, historical orders without an expiry and campaign reservations are excluded. Pending order expiry remains active when Render sleeps; email/announcement processing and live SSE updates still require an awake backend. The Java worker remains a fallback for local environments, disabled by `APP_MAINTENANCE_DATABASE_SCHEDULER=true` in production.

Monitoring runs at minutes 11 and 41 via GitHub Actions. It checks public authentication, durable audit recording, overdue orders, recent DEAD jobs, stalled delivery, cron failures and last successful cron executions. Failures mark the workflow red. Enable GitHub Actions failure notifications for the repository owner; delivery to a separate alert channel is not configured. Historical DEAD jobs retain a null `failed_at`; do not replay old emails automatically. Review and acknowledge their causes separately.

Encrypted backups run daily at **03:23 Asia/Ho_Chi_Minh**, retained 14 days in GitHub Actions. They include an internally consistent PostgreSQL dump and every Storage object body, key, size, MIME type and SHA-256, encrypted together using OpenSSL CMS AES-256-GCM and the checked-in public certificate. Storage objects are read one by one; uploads/deletions during the dump do not share the database snapshot, so restore a chosen consistent version before reopening uploads. Backup failure never uploads plaintext. These are snapshots, not PITR.

The private restore key is outside the repository at `~/.local/share/uitmerch-security-backups/restore-keys/operations-backup-private.pem`, in an owner-only directory. **Keep a separate offline copy**: neither GitHub nor Supabase can recover that key. Do not add it to Actions secrets or source control.

Download an `encrypted-production-backup-*` artifact, then:

```sh
python3 scripts/operations/verify-backup.py operations-backup.cms \
  --private-key ~/.local/share/uitmerch-security-backups/restore-keys/operations-backup-private.pem \
  --output /private/new-restore-bundle
# Create a fresh local PostgreSQL 17 + pgvector database first; never point this at production.
PGHOST=127.0.0.1 PGPORT=5432 PGDATABASE=uitmerch_restore_drill PGUSER=postgres \
  python3 scripts/operations/restore-drill.py /private/new-restore-bundle
```

`verify-backup.py` authenticates/decrypts the archive and checks every object checksum. `restore-drill.py` refuses remote/nonempty targets, recreates schema before installing extensions, then restores tables, functions, policies, triggers and indexes. It does not modify Supabase-managed auth/storage catalogs. To restore actual Storage files, recreate bucket access/size/MIME policies in a new project and upload the verified object bodies with their original keys and MIME types through S3. Apply grants with reviewed migrations and install cron separately; the dump intentionally omits ownership/ACL replay.

Supabase project availability still bounds database cron availability. GitHub schedules can be delayed; they are used for monitoring/backups, not the stock-expiry clock. Verify restore ability regularly and after schema changes.
