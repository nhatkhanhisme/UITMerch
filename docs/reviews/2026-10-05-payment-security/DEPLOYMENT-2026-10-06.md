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
