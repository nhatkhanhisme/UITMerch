# Security deployment — 2026-10-06

User authorized deployment after updating Gemini API key; old-key revocation at provider is not independently verified. Confirmed Render workspace My Workspace and Vercel project uitmerch (production alias uitmerch.vercel.app).

## Preflight completed

- Private custom-format PostgreSQL backup outside repository; archive readable, full public schema restore successful in isolated local PostgreSQL. Backup includes database metadata/data, not Storage object binaries.
- Live snapshot: 44 users / 29 orders. Test migration V44–V48 preserves 29 orders and one active verified admin with a non-seed password.
- All 43 applied production migration checksums match source. Historical Render failures were due to older mismatched migration files; no production repair or validation bypass performed.
- V48 enables public-table RLS without FORCE, revokes anon/authenticated table/sequence access and their default grants for migration owner. Backend table owner retains access. Existing service_role access is not revoked.
- Full PostgreSQL suite with V48: 315 tests, one migration-count expectation failed; remaining tests had no errors, one benchmark skipped. Updated expectation and strengthened RLS/grant assertions; migration-upgrade test rerun passed, validates 48 migrations.
- Previous frontend 66 unit / 94 browser tests and production build passed; no frontend app changes since that validation.
- GitNexus baseline reports aggregate CRITICAL scope: 198 changed indexed symbols / 112 affected flows. Newly added symbols are not indexed. Deployment monitoring and HTTPS verification remain required.
- Render production flags updated via merge preserving secrets. Environment update automatically triggered an old-main deploy; code deployment follows push to main.

## In progress

Push reviewed hardening to main, monitor Render and Vercel, then remove anonymous upload policies after authenticated backend uploads are available. Validate HTTPS cookie/proxy, anonymous access, migrations and scheduler. Strict script CSP and log retention require observation; do not mark them completed solely from config files.

## Confirmed production state

- Main commit `825c9ed` pushed after GitHub workflow authorization. Vercel production deployment `dpl_4S94xMFcdAZLda2tJXG2A41ivEpr` is ready and serves uitmerch.vercel.app; security headers verified on HTTPS preview and production source is the same commit.
- All GitHub test/security jobs passed: 315 backend tests, no failures/errors, one opt-in benchmark skipped; frontend and dependency/secret scans successful. First deploy-readiness job failed while environment issues were being corrected; that failure is retained as evidence.
- Flyway completed V48; 34/34 public tables have RLS, 44 users and 29 orders preserved, zero active unchanged-seed-password accounts. One active verified admin remains.
- Two anonymous upload INSERT policies removed; anonymous requests to avatars and org-assets now rejected with access-control status 403. Public media read policies retained. Four image buckets capped at 10 MiB and JPEG/PNG/WebP/GIF.
- HTTPS API auth checks pass: Secure/HttpOnly/Lax cookie on frontend origin and auth path, missing CSRF and foreign Origin rejected, refresh rotation, authenticated cart, logout and refresh rejection. Browser login/reload and token absence from localStorage/document.cookie also pass.
- Render startup recovery: session advisory locks require session-mode Flyway connection, with project-qualified username and a separate password when runtime JDBC URL embeds credentials. Dedicated Flyway credentials now configured; validation remains enabled. One idle JDBC session holding the verified Flyway history lock was terminated; no active application transaction was terminated.
- Render S3 credentials were invalid. Local S3 credentials were verified by a signed read-only S3 request, then transferred through the tool orchestration into Render without displaying values. Latest S3 environment deploy is live. Valid owner upload returns 200; SVG returns 400; anonymous backend upload returns 401. Probe image removed using signed S3 DELETE (204).
- Main branch now requires backend/frontend/security status checks, up-to-date branch and administrator enforcement; force pushes/deletion disabled. No human review requirement added. Future changes must reach main with successful checks.

Provider revocation of old Gemini keys is still user-reported only; key update does not independently prove revocation. Strict script/connect CSP, vector/pg_trgm extension warnings and longer-term centralized audit-log retention remain follow-up checks.

## Final HTTPS evidence

See [production-verification.json](production-verification.json). Real-browser login, cookie restore after reload, logout redirect and upload checks pass. No CSP violations observed in the tested customer flows; this does not cover every role/flow. SMTP challenge returned 202 to the configured sender mailbox; inbox receipt was not independently checked. Anonymous Data API access to users/orders/auth_sessions/checkout_actors returns 401. All temporary probe accounts and the uploaded probe object were removed; 44 users and 29 orders remain.

GitHub run [37346580317](https://github.com/nhatkhanhisme/UITMerch/actions/runs/37346580317) is now fully green after rerunning only the failed deploy/readiness job. The hook started another deployment of the same application commit and environment; final status is monitored separately. Documentation/environment examples in this working tree record the live recovery and are not application changes deployed in commit 825c9ed.

Final deploy `dep-db1u713ncjis73ava27g` is LIVE at commit 825c9ed. Frontend proxy CSRF and public campaign API both return 200 JSON after the rerun. Current source secret scan remains zero findings. Application deployment is complete; remaining follow-ups are explicitly listed in the verification artifact.
