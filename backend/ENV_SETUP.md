# Environment setup

Run the commands below from `backend/` with Java 21.

| Mode | Command | Database | Storage/email | `.env` |
|---|---|---|---|---|
| Local dev | `APP_ENVIRONMENT=local ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | In-memory H2; reset on shutdown | Mock storage; real SMTP unless explicitly opted out | Optional |
| Docker Compose | `docker compose up --build` | External PostgreSQL from `.env`; no database container included | Real Supabase/SMTP by default | Required |
| Default / prod | `./mvnw spring-boot:run` or `java -jar target/backend-*.jar --spring.profiles.active=prod` | External PostgreSQL | Real Supabase/SMTP | Required locally, or inject environment variables |
| Explicit docker profile | `APP_ENVIRONMENT=local SPRING_PROFILES_ACTIVE=docker docker compose up --build` | External PostgreSQL | Mock storage; real SMTP unless explicitly opted out | Required for database and JWT |

Docker packaging does not automatically activate the `docker` Spring profile. That profile is for development; do not activate it on a production service because it enables mock services and development endpoints/data initialization.

## Credentials

Copy `.env.example` to `.env` and fill in PostgreSQL, SMTP, Supabase Storage and JWT values. Generate a signing secret with `openssl rand -base64 48`. Keep `.env` out of Git and Docker images. Compose passes its values into the container at runtime through `env_file`; Java launched from `backend/` loads `.env` through `spring-dotenv`. For a JAR launched elsewhere, inject environment variables or provide `.env` in the process working directory.

Required for real services:

- `APP_JWT_SECRET`: a strong signing secret, minimum 32 characters.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`: PostgreSQL connection details. The database must support pgvector, including extension creation for V29.
- `MAIL_USERNAME`, `MAIL_PASSWORD`: SMTP account credentials. `MAIL_SMTP_AUTH` defaults to `true`.
- `SUPABASE_STORAGE_ENDPOINT`, `SUPABASE_STORAGE_REGION`, `SUPABASE_STORAGE_S3_ACCESS_KEY_ID`, `SUPABASE_STORAGE_S3_SECRET_KEY`, `SUPABASE_PROJECT_URL`: Storage S3 credentials and public URL.

`GEMINI_API_KEYS` (comma-separated) or `GEMINI_API_KEY` enables AI requests. Empty keys do not block normal backend startup, but AI services remain unavailable. Configure real keys locally or in the deployment environment; never put them into an image or commit.

## Ports and optional settings

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | Falls back to `SERVER_PORT` | Takes precedence for HTTP, Compose published port, and healthcheck |
| `SERVER_PORT` | `8080` | HTTP port when `PORT` is absent |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Exact allowed frontend origins, comma-separated |
| `APP_JWT_EXPIRATION` | `86400000` | Access token lifetime in milliseconds |
| `APP_JWT_REFRESH_EXPIRATION` | `604800000` | Refresh token lifetime in milliseconds |
| `APP_TRUSTED_PROXY_IPS` | Empty | Trusted proxy IPs for forwarded client addresses |
| `SWAGGER_ENABLED` | `false` | Enable `/swagger-ui.html` and `/v3/api-docs` in deployed profiles; dev enables them |
| `APP_MAIL_FROM_NAME` | `UITMerch` | Email sender display name |

For example, with `SERVER_PORT=18080`, Compose exposes `http://localhost:18080` and checks the same port internally. With `PORT=19090` as well, all three use `19090`. Shell environment variables take precedence over the Compose `.env` file. When selecting a separate env file, use `docker compose --env-file <path>` for interpolation and override the service's `env_file` to that path as well.

`APP_CORS_ALLOWED_ORIGINS` replaces the default list when set. Include the exact origin where you open the frontend: `http://localhost:5173` and `http://127.0.0.1:5173` are different origins. Origins contain scheme, hostname and optional port; omit paths and the trailing `/`. For local development alongside Vercel, append your deployed origin to the localhost entries in `.env.example`, for example `https://your-frontend-domain.vercel.app`. A backend healthcheck can pass while browser API requests fail with CORS 403 because healthchecks do not send the frontend's `Origin` header.

After editing `backend/.env`, recreate the backend container from `backend/` so Compose injects the updated values:

```bash
docker compose up -d --no-build --force-recreate backend
```

Changing runtime CORS configuration requires container recreation; no image rebuild is needed. For direct API calls, set frontend `VITE_API_BASE_URL=http://localhost:8080` in local `.env`; Vercel builds require the externally reachable backend URL. Restart Vite after changing its environment file, and rebuild a deployed frontend after changing its build variables.

## Safe local development

The `dev` profile always binds its datasource and Hikari connection settings to in-memory H2, even if your shell contains production `SPRING_DATASOURCE_*` values. This prevents the H2/PostgreSQL driver mismatch and keeps development schema creation away from the remote database. To choose a different in-memory database, set `UITMERCH_DEV_DATASOURCE_URL=jdbc:h2:mem:other;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`; quote this value in a shell. Persistent or PostgreSQL URLs are rejected for this setting. Use the default profile to exercise PostgreSQL/Flyway.

Development tools are disabled by default. In a local dev/docker environment only, explicitly set `APP_DEV_OTP_ENDPOINT=true` for `/api/v1/dev/otps`, `APP_DEV_SEED_DATA=true` for demo seed data, or `APP_DEV_MOCK_MAIL=true` to suppress SMTP. Mock mail never logs OTPs. Bind development servers to loopback and do not expose these tools publicly. Production/staging reject dev/docker profiles and all three flags at startup, including on Render even if `APP_ENVIRONMENT=local` was mistakenly set.

`APP_ENVIRONMENT` defaults to `production`; supported values are `local`, `test`, `staging`, and `production`. Tests set their own environment. Deployment with dev flags, H2 console or destructive Hibernate schema updates fails before beans are created.

V46 disables accounts still using the exact public V12/V14 demo password hash, revokes their sessions, and invalidates outstanding OTPs. It preserves orders, organizations and users who changed their password. Before rollout, provision a verified administrator with a unique password and a tested recovery procedure; do not re-enable an account while it retains the demo hash. Production credential rotation and recovery must use the owner's access to the provider.

Email delivery in real profiles uses durable background jobs and retry processing; it does not send synchronously in the request transaction.

## Migration recovery

Current schema version: **V43**. Do not reuse applied migration versions or disable Flyway validation to bypass a mismatch.

The historical hosted database used V33 for guest address removal and V34 for OTP purpose. These scripts are restored verbatim. The new authentication/delivery/features migrations now occupy V35–V43. For the known original V16 checksum, the only SQL change is its missing semicolon. Run:

```bash
./scripts/flyway-maintenance.sh validate
# Only if validation reports the known V16 checksum mismatch:
./scripts/flyway-maintenance.sh repair-v16
./scripts/flyway-maintenance.sh migrate
```

The repair command refuses unknown checksums, unknown resolved V16 content, or any additional validation failures. It backs up schema history before using Flyway repair. Set `UITMERCH_MIGRATION_BACKUP_DIR` to a persistent private backup directory if needed; its default is `/tmp`. Run maintenance while other deployments are stopped. An environment that already applied the superseded authentication V33/delivery V34 lineage needs separate reconciliation; this command deliberately refuses that lineage.

## Parsing and verification

Use one `KEY=value` assignment per line. Base64 `=`, `/` and `+` characters in values are supported. Remove malformed lines instead of relying on YAML `dotenv.ignoreIfMalformed`: spring-dotenv 4 reads its configuration from a classpath `.env.properties` file, not those YAML keys.

Check `docker compose ps` for a healthy backend and request `/api/v1/public/events?size=1`. Compose and the Dockerfile use this endpoint for healthchecks. Do not print `docker compose config`, container environments or application properties containing real credentials into shared logs. A healthy endpoint verifies startup and database access; it does not prove SMTP, Storage uploads or Gemini requests work.

If Docker reports `no space left on device`, free host disk space before rebuilding. On Arch, `sudo paccache -rk2` keeps two cached versions per package; it does not uninstall packages. Avoid deleting database volumes. A runtime `.env` should never be copied into the Docker build context.

## Cookie auth and checkout rollout

Production frontend auth uses the same-origin `/api` reverse proxy configured in `frontend/vercel.json`. Use exact HTTPS frontend origins for `APP_CORS_ALLOWED_ORIGINS`; production refuses localhost HTTP origins. Keep `APP_AUTH_COOKIE_SECURE`, `APP_RATE_LIMIT_SHARED`, and `APP_CHECKOUT_EXPIRY_ENABLED` true. Local HTTP development must explicitly use `APP_ENVIRONMENT=local` and `APP_AUTH_COOKIE_SECURE=false`; never reuse a local signing key in a deployed environment.

Checkout defaults are `APP_CHECKOUT_MAX_QUANTITY=10`, `APP_CHECKOUT_MAX_PENDING=3`, and `APP_CHECKOUT_PENDING_HOURS=48`. Configure real SMTP and validate email delivery before accepting guest checkout. See [security rollout](../docs/reviews/2026-10-05-payment-security/ROLLOUT.md) for V46/V47 migrations, Supabase privileges and rollback gates.

Flyway on Supabase must use a direct or session-mode connection when using session advisory locks for concurrent index migrations. Configure `SPRING_FLYWAY_URL` on pooler port 5432 and its project-qualified `SPRING_FLYWAY_USER`; set `SPRING_FLYWAY_PASSWORD` separately if runtime credentials are embedded in `SPRING_DATASOURCE_URL`. Do not use transaction pooler port 6543 for this migration connection. Keep migration validation enabled.

### Durable security audit retention

`APP_AUDIT_RETENTION_DAYS` defaults to 90; valid values are 1–365. Flyway V50 creates `security_audit.events` in a private schema with RLS and no browser-role grants. Security mutations record route templates, actor UUIDs, HTTP status and validated trace IDs only. An hourly job deletes at most 5,000 expired rows per run; retention cleanup resumes after a sleeping Render instance wakes. PostgreSQL persistence failures increment an internal metric and retain the redacted stdout event without changing the HTTP outcome. The backend database owner can administer these rows; this is durable retention, not immutable external storage.

V49 moves vector and pg_trgm to `extensions` without rebuilding indexes. Native vector writes/searches qualify types and operators explicitly to work through transaction pooling. Hikari sets `public, extensions` for other trusted extension queries; local H2 disables that initialization. Historical migration checksums are unchanged.
