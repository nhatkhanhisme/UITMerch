# Environment Setup

> For most cases (local development or frontend testing) you do **not** need a `.env` file.
> See the table below to pick the right mode.

## Which mode should I use?

| Mode | Command | Database | Storage | Email | `.env` needed? |
|---|---|---|---|---|---|
| **Docker** | `docker compose up --build` | PostgreSQL (container) | Mock | No-op (OTPs logged) | No |
| **Dev** | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` | H2 in-memory | Mock | No-op (OTPs logged) | No |
| **Production** | `mvn spring-boot:run` | PostgreSQL (external) | Supabase | JavaMail (SMTP) | **Yes** |

---

## Production Setup

### 1. Copy the template

```bash
cp .env.example .env
```

### 2. Fill in required variables

```env
# ── JWT ───────────────────────────────────────────────────────────────────────
# Minimum 32 characters — validated at startup; use openssl rand -base64 48
APP_JWT_SECRET=<min 32 random chars>

# ── PostgreSQL ────────────────────────────────────────────────────────────────
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/uitmerch
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# ── Gmail SMTP ────────────────────────────────────────────────────────────────
MAIL_USERNAME=your-address@gmail.com
MAIL_PASSWORD=<Gmail App Password — not your account password>

# ── Supabase Storage ──────────────────────────────────────────────────────────
SUPABASE_STORAGE_ENDPOINT=https://<project-ref>.supabase.co/storage/v1/s3
SUPABASE_STORAGE_REGION=ap-southeast-2
SUPABASE_STORAGE_S3_ACCESS_KEY_ID=<from Supabase dashboard>
SUPABASE_STORAGE_S3_SECRET_KEY=<from Supabase dashboard>
SUPABASE_PROJECT_URL=https://<project-ref>.supabase.co
```

### 3. Generate a JWT secret

```bash
openssl rand -base64 48
```

The secret must be **at least 32 characters** (256 bits). The application will refuse to start if the secret is too short.

### 4. Start the application

```bash
./mvnw spring-boot:run
# or: java -jar target/backend-*.jar
```

---

## All Variables Reference

### Server

| Variable | Default | Required | Description |
|---|---|---|---|
| `SERVER_PORT` | `8080` | No | HTTP listening port |

### JWT

| Variable | Default | Required | Description |
|---|---|---|---|
| `APP_JWT_SECRET` | — | **Yes** | HMAC-SHA256 signing secret — minimum 32 chars; validated at startup |
| `APP_JWT_EXPIRATION` | `86400000` | No | Access token TTL in ms (default: 24 h) |
| `APP_JWT_REFRESH_EXPIRATION` | `604800000` | No | Refresh token TTL in ms (default: 7 days) |

### CORS

| Variable | Default | Required | Description |
|---|---|---|---|
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | No | Comma-separated list of allowed CORS origins |

### Rate Limiting / Proxy

| Variable | Default | Required | Description |
|---|---|---|---|
| `APP_TRUSTED_PROXY_IPS` | *(empty)* | No | Comma-separated IPs (e.g. your load-balancer) whose `X-Forwarded-For` header is trusted for per-IP rate limiting. Leave blank when running without a reverse proxy. |

### Swagger / API Docs

| Variable | Default | Required | Description |
|---|---|---|---|
| `SWAGGER_ENABLED` | `false` | No | Set to `true` to enable Swagger UI (`/swagger-ui.html`) and OpenAPI schema (`/v3/api-docs`) in production |

### Database

| Variable | Default | Required | Description |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/uitmerch` | No | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | No | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | No | DB password |

### Mail (production profile only)

| Variable | Default | Required | Description |
|---|---|---|---|
| `MAIL_USERNAME` | — | **Yes** | Gmail address used as the SMTP sender |
| `MAIL_PASSWORD` | — | **Yes** | Gmail App Password (enable 2FA, then generate at myaccount.google.com → Security → App passwords) |
| `APP_MAIL_FROM_NAME` | `UITMerch` | No | Display name in the email `From:` header |
| `MAIL_SMTP_AUTH` | `true` | No | — |

> All email methods are dispatched `@Async` — they never block the request thread. If SMTP fails, a warning is logged but the triggering operation (order update, registration) succeeds.
>
> In `dev` and `docker` profiles, `DevEmailService` is active — it logs OTPs to the console instead of sending email. OTPs are also readable at `GET /api/v1/dev/otps?email=<email>`.

### Supabase Storage (production profile only)

| Variable | Required | Description |
|---|---|---|
| `SUPABASE_STORAGE_ENDPOINT` | **Yes** | S3-compatible endpoint from Supabase Storage settings, e.g. `https://<project-ref>.supabase.co/storage/v1/s3` |
| `SUPABASE_STORAGE_REGION` | **Yes** | AWS region string, e.g. `ap-southeast-2` |
| `SUPABASE_STORAGE_S3_ACCESS_KEY_ID` | **Yes** | From Supabase dashboard → Storage → S3 credentials |
| `SUPABASE_STORAGE_S3_SECRET_KEY` | **Yes** | Same location |
| `SUPABASE_PROJECT_URL` | **Yes** | Public base URL, e.g. `https://<project-ref>.supabase.co` |

> In `dev` and `docker` profiles, `DevStorageService` returns placeholder URLs and performs no actual file operations.

---

## Dotenv Notes

The backend uses `spring-dotenv` to load `.env` automatically. If your secret values contain base64-encoded strings (with `=`, `/`, or `+`), make sure each line follows the exact format:

```
KEY=value
```

No spaces around `=`, no quotes needed. If you see a startup warning about malformed entries, check for lines that don't follow this pattern.

The application ignores missing or malformed entries gracefully (`dotenv.ignoreIfMalformed=true`, `dotenv.ignoreIfMissing=true` in `application.yaml`).

---

## Security Notes

- Never commit `.env` to version control — it is in `.gitignore`
- Use different credentials for dev, staging, and production
- Rotate Supabase S3 keys periodically from the Supabase dashboard
- The JWT secret must be **at least 32 characters** — the application fails fast at startup if it is too short
- The token blacklist is stored in PostgreSQL — logged-out tokens remain invalid across server restarts
