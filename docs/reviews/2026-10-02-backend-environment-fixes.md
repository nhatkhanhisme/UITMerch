# Backend environment fixes — 2 October 2026

Follow-up to [the environment audit](2026-10-02-backend-environment-audit.md). Source fixes and database recovery are implemented on `refactor/fe`. Docker verification was initially blocked by host disk space; the [3 October follow-up](2026-10-03-docker-validation.md) verifies the rebuilt image, healthy startup and 265 backend tests after fixing public campaign pagination. Results below record the original 2 October validation.

## Fixes

- Restore deployed `V33__drop_guest_address.sql` and `V34__add_otp_purpose.sql` verbatim from Git history. Their checksums match the hosted database. Authentication, delivery and feature migrations are renumbered to V35–V43 without changing their SQL bodies.
- Remove `Order.guestAddress` persistence mapping, which referenced the column removed by deployed V33. Keep the legacy response field as `null`, and stop assigning addresses in development seed orders. Existing checkout/order/pickup/campaign tests exercise the corrected schema.
- Add explicit `scripts/flyway-maintenance.sh` commands to validate, repair the known V16 checksum, and migrate. Repair requires the original applied checksum `42806920`, the reviewed resolved checksum `1114743976`, and a validation report limited to that mismatch. It saves history before calling Flyway repair. Unknown revisions and unrelated migration errors are refused; repair never runs automatically during application startup.
- Align Compose published/container ports and healthcheck with `PORT`, falling back to `SERVER_PORT` and then 8080. Set both container variables to the selected port. The startup listener resolves `PORT` after dotenv loading so Spring's direct `SERVER_PORT` environment binding cannot override it; explicit command-line/JVM `server.port` remains authoritative.
- Pin the dev datasource and Hikari settings to in-memory H2 before bean creation, protecting dev schema generation from exported production datasource values. An optional `UITMERCH_DEV_DATASOURCE_URL` must remain in-memory H2.
- Honor JWT lifetime placeholders in the docker profile. Remove ineffective YAML/JVM dotenv tolerance flags and correct [environment setup](../../backend/ENV_SETUP.md), root/backend READMEs and migration documentation.

## Database recovery performed

The configured database actually contained the old V33/V34 lineage, not authentication sessions/background delivery. It was backed up using `pg_dump` of the application's public schema and data. A restored disposable clone exercised the recovery first:

- Additional migration mismatch and unknown V16 checksum: refused.
- Known V16 repair: succeeded; no application tables changed by repair.
- V34 → V43: nine migrations applied. A repeated migrate applied zero migrations.
- Users: 40; orders: 22; events: 7; merch items: 43; aggregate stock: 1391 before and after.
- Historical announcement rows: zero after migration.

The same guarded repair and nine migrations were then applied to the database configured in `backend/.env`. Its latest migration is V43, successful, with pgvector available. No SMTP messages, Storage uploads/deletes or frontend writes were used as smoke tests. The newly added functions are deployed in the schema; API checks remain read-only.

Private pre-upgrade schema/data and history backups are outside Git at `/home/nhatkhanhh/.local/state/uitmerch/migration-recovery-2026-10-02/`, with private permissions. Recovery logs remain private temporary files. The committed [validation artifact](2026-10-02-backend-environment-fixes.json) contains test totals, class names, booleans and aggregate counts, not credentials or application rows.

## Verification

The final suite passed **264 tests across 30 classes**, with zero failures, errors and skips. Test totals and runtime results are recorded in the linked validation artifact. The complete backend suite uses isolated PostgreSQL 18.6 + pgvector 0.8.2, compiled and run in `/tmp` because Docker cannot create a container on the full host filesystem. Fresh migrations and the deployed V34 upgrade are covered. The hosted database is also verified directly after recovery; this does not replace a Docker image build/runtime check.

`default`, `docker` and `prod` startup smoke tests run against the configured database with Flyway enabled. They force database connections read-only and exclude scheduled processing, embedding backfill and development seeding to avoid application writes. Public events return 200. Default/prod select real SMTP/Supabase implementations; docker selects development mocks. OpenAPI returns 401 because `SWAGGER_ENABLED=false`, as intended.

The packaged dev JAR starts with production datasource environment values present, uses H2, and returns 200 for public events and OpenAPI. Direct environment values select port 19090 despite SERVER_PORT=18080; a local dotenv fixture selects port 19191 despite SERVER_PORT=18181. Concurrent startup probes initially exceeded their timeouts under host load; the final probes run with fewer JVMs and bounded JVM resources. Environment checks load all 17 audited variables; deployed profiles bind all 17 expected values. Dev intentionally replaces datasource, mail and storage settings. TTL overrides are honored in every profile. The JAR excludes `.env` and contains no configured JWT, database, SMTP or S3 secrets in application classes/resources.

## External blockers at the time of this review

The full-disk Docker blocker below is resolved in the [3 October validation](2026-10-03-docker-validation.md). Gemini keys remain intentionally empty.

- `/` is full on Btrfs. `/var/cache/pacman/pkg` contains approximately 31 GB of package cache. Clearing system cache requires the user's sudo access: `sudo paccache -rk2` retains two versions of each package. Docker build/create/prune metadata writes fail with ENOSPC, so a fresh image and container healthcheck cannot yet be verified.
- The previous audit's four build-cache IDs remain pending cleanup: `jx58ehtbsw7arv1z45vblu5yq`, `a001t29k90kb5vtiuumcifwow`, `x1upcx7lvnawi6oy7jn9cvsr2`, `gzt8bpkpgge20s2h7ranij65i`. Cleanup is scoped to these IDs. No user images or database volumes were removed. Two package build-cache directories were preserved at `/tmp/uitmerch-package-cache-backup/`; moving them did not free usable Btrfs space.
- Both Gemini key variables are empty. Configure a real key locally/in the deployment environment to enable AI requests; normal backend startup works without it.
- No remote Render/Vercel deployment was triggered. This change checks backend local/configured-database startup and deployment configuration behavior.

## Impact review

GitNexus reports `Order` as MEDIUM, with seven direct references. `OrderResponse.from` is CRITICAL, with nine direct callers and seventeen affected order/checkout/pickup processes; the warning was reported before edits. Staged change detection confirms the expected mapper/order flows. Startup/configuration symbols have incomplete graph coverage; their absence of graph callers is treated as UNKNOWN and backed by runtime tests.

Reference behavior: [Flyway repair](https://documentation.red-gate.com/flyway/reference/commands/repair) changes schema history; it does not apply missing application DDL. Spring's [environment customization](https://docs.spring.io/spring-boot/3.3/api/kotlin/spring-boot-project/spring-boot/org.springframework.boot.env/-environment-post-processor/index.html) supports ordered processing before configuration binding. The direct PORT/SERVER_PORT conflict was reproduced with the packaged JAR before fixing startup precedence.
