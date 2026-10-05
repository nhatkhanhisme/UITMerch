# Operations hardening — 2026-10-06

Work is isolated in `UITMerch-ops-hardening`, branch `security/operations-hardening`. Preserve the concurrent local `OrganizationRepository.java` change and previous local review evidence in the original worktree.

Implemented:

- V51 creates separate runtime/backup/monitor identities without passwords in source. Runtime permits business DML through role-specific RLS policies, appends audit only, cannot modify schema/roles or Flyway history, and cannot bypass RLS. Backup reads protected data; monitor can execute fixed aggregate/probe functions only.
- V52 adds private database maintenance, atomic COD expiry with inventory/restock/history/pickup/notification effects, independent audit retention, and failure timestamps for future DEAD jobs. Supabase cron installation is separate from portable Flyway migrations.
- Runtime database maintenance flag disables application-only expiry/retention workers when database scheduling is active. ASYNC servlet completion is allowed after initial authenticated SSE dispatch; per-event revocation remains enforced.
- Trusted main CI deploy validates/migrates with credentials isolated in a protected-branch `production` environment before triggering Render. Render Auto-Deploy is verified Off.
- Production monitoring and daily encrypted off-provider database/Storage backups, with decryption/checksum/local database restore scripts. Backup plaintext never enters Actions artifacts. The private restore key stays outside Git and CI.
- Tailwind 4 replaces Tailwind 3's braces dependency. All vulnerability exceptions removed. Compatibility typography, shadows, radii and focus outlines preserve existing UI. Desktop/mobile comparisons retain text/form geometry; the home scroll button is now correctly centered because animation and horizontal translation compose.
- Online-payment implementation plan records approved guest/COD/auth constraints, multi-organization merchant decisions, immutable ledger, signed/idempotent webhooks, stock locking, reconciliation/refunds and rollout gates. No payment feature is enabled.

Validation before rollout:

- Backend full run: 320 tests, zero failures/errors, one optional benchmark skipped; additional permission/failure-timestamp test passed separately.
- Frontend unit: 66 passed. Production CSP/browser: 95 passed after final typography compatibility refinement.
- npm/Maven scanner: zero unresolved advisories and zero exceptions. Locked Python backup SDK: no known vulnerabilities. Source secret scan: clean.
- Pre-migration private database backup: V50. Restored schema and Flyway Maven plugin migration validated against disposable PostgreSQL 17.
- Full encrypted backup captured 4 buckets and 95 Storage object bodies (37,305,463 bytes). Decryption and all 95 object checksums verified. Restored encrypted database contains 44 users, 29 orders, 38 embeddings and 15 audit events, with zero invalid indexes. Tampered ciphertext rejected.
- Live old-key revocation is confirmed by the user, not independently verified through Google key-management APIs.

Existing operational debt: 90 historical DEAD delivery jobs. They are preserved for triage; old emails are not replayed automatically. An independent scheduler persists notifications/restock work while Render sleeps; delivery/email/SSE latency still depends on the application worker. The restore key needs a separate offline copy. Alerts currently surface as failed GitHub workflows; a separate delivery channel is not configured.

Production rollout results will be recorded after PR gates, migration, role provisioning, cron installation, runtime credential cutover and live verification complete.
