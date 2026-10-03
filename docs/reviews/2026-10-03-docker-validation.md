# Docker and function validation — 3 October 2026

Follow-up to [the backend environment fixes](2026-10-02-backend-environment-fixes.md). Docker build and startup are now verified after host disk space was freed. Both `GEMINI_API_KEY` and `GEMINI_API_KEYS` remain empty as requested. The rebuilt `uitmerch-backend` container is left running at **http://localhost:8080**, healthy with zero restarts.

## Runtime bug fixed

The actual container returned 500 for `GET /api/v1/public/campaigns?size=1`. Hibernate rendered the Java enum literal as `'ACTIVE'::OrganizationStatus`, while Flyway creates the PostgreSQL type `organization_status`. PostgreSQL consequently reported that type `organizationstatus` does not exist.

`CampaignRepository.findPublic` now binds the active organization enum through a Spring Data expression parameter, matching the working merchandise/event queries. No database schema change is required. A new PostgreSQL MockMvc regression failed with 500 before the fix and passes afterward. It creates two visible campaigns and one campaign belonging to an inactive organization, checks both one-item pages and their total count, and rolls its fixtures back.

GitNexus upstream impact is LOW: one direct caller, `CampaignService.publicList`, and the public campaign controller list flow. Staged detection reports only the campaign repository/test files and the expected list process. Its older line positions also attribute the inserted test to adjacent test methods; the reviewed diff leaves those methods unchanged. Fix commit: `8403266`.

Reference: Hibernate documents [HQL enum literals and parameters](https://docs.hibernate.org/orm/6.5/querylanguage/html_single/). The erroneous PostgreSQL cast and its corrected API behavior were reproduced locally.

## Results

| Check | Result |
| --- | --- |
| Full backend suite | **265 tests**, 30 classes; zero failures, errors or skips |
| Test database | Disposable PostgreSQL 17 Docker container with pgvector and Flyway migrations |
| Fresh backend image build | Pass; Java 21, multi-stage image, runtime user `spring` |
| Compose startup | Running and healthy on port 8080; zero restarts |
| `.env` injection | **17/17** audited variables match the container environment |
| Gemini configuration | Both keys empty; normal startup and other API functions work |
| Packaged application audit | No `.env` or configured JWT/database/mail/S3 secrets in application resources; 43 migration files |
| Actual container HTTP smoke | **23/23** expected responses |
| Frontend unit/component tests | **40/40**, six files |
| Frontend browser tests | **45/45**, one worker |
| Frontend production build | Pass; existing warning about chunks larger than 500 kB |

The [sanitized JSON artifact](2026-10-03-docker-validation.json) records per-class backend totals, container/image metadata, environment match booleans and each HTTP status. Credentials and application response bodies are excluded.

HTTP checks cover paginated events, merchandise, organizations and campaigns; keyword/popular merchandise; categories; detail endpoints; purchase context; and merchandise/events within an organization. Public requests return 200. Protected customer/admin routes reject unauthenticated access with 401. OpenAPI/Swagger also return 401 because the configured `SWAGGER_ENABLED=false` remains in effect.

Backend tests exercise authentication/session handling, authorization, checkout/stock concurrency, order cancellation/history, pickup, restock alerts, following, analytics, preorder reservation/finalization, campaign context and migration upgrades. They use an isolated database and mocked mail/embedding transports. The test container is removed automatically; backend Compose remains running.

Frontend browser tests use API route mocks. An earlier run during concurrent Docker/Maven work timed out waiting for a checkbox in the two-tab Web Locks test, passing 44/45 scenarios. That scenario subsequently passed three repeated runs, and the final full run passed all 45 scenarios without frontend source changes.

## Commands

From `backend/`:

```bash
UITMERCH_TEST_POSTGRES_IMAGE=public.ecr.aws/supabase/postgres:17.6.1.167 \
  JDK_JAVA_OPTIONS='-XX:ActiveProcessorCount=2 -Xmx768m' \
  ./scripts/test-postgres.sh -q
docker compose build backend
docker compose up -d --no-build backend
docker compose ps
```

The image override uses the compatible PostgreSQL image already cached on this host. The test harness defaults to `pgvector/pgvector:pg17` and excludes the project's `.env` from its temporary build.

From `frontend/`:

```bash
npm test
npm run test:e2e
npm run build
```

## Verification limits

Real Gemini requests are not tested while keys are empty. SMTP delivery and Storage mutations use test substitutes; no test email or upload is sent against the configured external services. Container API checks issue GET requests only. The configured database has no public campaign rows, so the seeded pagination/count and visibility behavior is verified in the isolated PostgreSQL regression. No remote Render/Vercel deployment is triggered.
