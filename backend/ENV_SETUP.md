# Environment Setup

> For most cases (local development or frontend testing) you do **not** need a `.env` file.
> See the table below to pick the right mode.

## Which mode should I use?

| Mode | Command | Database | Storage | `.env` needed? |
|---|---|---|---|---|
| **Docker** | `docker compose up --build` | PostgreSQL (container) | Mock | No |
| **Dev** | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` | H2 in-memory | Mock | No |
| **Production** | `mvn spring-boot:run` | PostgreSQL (external) | Supabase | **Yes** |

---

## Production Setup

### 1. Copy the template

```bash
cp .env.example .env
```

### 2. Fill in required variables

```bash
# ── JWT ──────────────────────────────────────────────────────────────────────
APP_JWT_SECRET=<min 32 random chars — see generation command below>

# ── PostgreSQL ────────────────────────────────────────────────────────────────
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/uitmerch
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

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

### 4. Start the application

```bash
mvn spring-boot:run
# or: java -jar target/backend-*.jar
```

---

## All Variables Reference

### Server

| Variable | Default | Required |
|---|---|---|
| `SERVER_PORT` | `8080` | No |

### JWT

| Variable | Default | Required |
|---|---|---|
| `APP_JWT_SECRET` | — | **Yes (prod)** |
| `APP_JWT_EXPIRATION` | `86400000` (24 h) | No |
| `APP_JWT_REFRESH_EXPIRATION` | `604800000` (7 days) | No |

### Database

| Variable | Default | Required |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/uitmerch` | No |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | No |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | No |

### Supabase Storage (production only)

| Variable | Required | Notes |
|---|---|---|
| `SUPABASE_STORAGE_ENDPOINT` | **Yes** | S3-compatible endpoint from Supabase Storage settings |
| `SUPABASE_STORAGE_REGION` | **Yes** | e.g. `ap-southeast-2` |
| `SUPABASE_STORAGE_S3_ACCESS_KEY_ID` | **Yes** | From Supabase dashboard → Storage → S3 credentials |
| `SUPABASE_STORAGE_S3_SECRET_KEY` | **Yes** | Same location |
| `SUPABASE_PROJECT_URL` | **Yes** | e.g. `https://<project-ref>.supabase.co` |

> These five variables are **not needed** in `dev` or `docker` profiles — `DevStorageService` handles storage there and returns placeholder URLs.

---

## Security Notes

- Never commit `.env` to version control — it is in `.gitignore`
- Use different credentials for dev, staging, and production
- Rotate Supabase S3 keys periodically from the Supabase dashboard
- Use a strong, unique `APP_JWT_SECRET` in production (≥ 256 bits)
