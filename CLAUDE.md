# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

```
UITMerch/
├── backend/    Spring Boot 3.3 · Java 21 · PostgreSQL · Flyway · JWT
└── frontend/   React 18 · TypeScript · Vite · Tailwind · Zustand · React Query
```

## Commands

### Backend (run from `backend/`)

| Command | Purpose |
|---|---|
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | Run with H2 in-memory DB — no env vars needed |
| `./mvnw spring-boot:run` | Run with PostgreSQL — requires `.env` |
| `docker compose up --build` | Docker (PostgreSQL + sample data, no env vars) |
| `./mvnw test` | Run tests (uses dev/H2 profile) |
| `./mvnw clean package` | Build artifact |

On Windows replace `./mvnw` with `.\mvnw.cmd`.

API: `http://localhost:8080` | Swagger: `http://localhost:8080/swagger-ui.html`

### Frontend (run from `frontend/`)

| Command | Purpose |
|---|---|
| `npm run dev` | Vite dev server (`http://localhost:5173`) |
| `npm run build` | Type-check + production bundle |
| `npm run preview` | Preview production build |

## Backend architecture

See `backend/CLAUDE.md` for the authoritative backend conventions.

### Profiles

- **`dev`** — H2 in-memory, Flyway disabled, Hibernate `create-drop`, stub email/storage. No `.env` needed. OTPs retrievable at `GET /api/v1/dev/otps?email=<email>`.
- **`docker`** — PostgreSQL via Docker Compose, sample data auto-seeded, stub email/storage.
- **default** — PostgreSQL + Supabase Storage + JavaMail. Requires `.env` (copy `.env.example`).

### Domain modules

`auth` · `user` · `organization` · `merch` · `cart` · `order` · `wishlist` · `event` · `notification` · `admin` · `common`

Each follows: `entity/ → repository/ → dto/ → service/ → controller/`

### Dual-implementation services

Both swap automatically via `@Profile`:

- **`StorageService`** — `DevStorageService` (dev/docker, no-op) vs `SupabaseStorageServiceImpl` (production S3-compatible). Never store images as Base64/BLOB in the DB.
- **`EmailService`** — `DevEmailService` (dev/docker, logs OTP to console) vs `JavaMailEmailService` (production SMTP, all methods `@Async`). Three methods: `sendOtp`, `sendPasswordReset`, `sendOrderStatusUpdate`.
- **`NotificationService`** — creates in-app `Notification` records for CUSTOMER users (e.g., order moved to `READY`). Queried via `GET /api/v1/customer/notifications`; supports mark-read and unread-count.

### Security

`JwtAuthenticationFilter` validates the `Authorization: Bearer <token>` header on every request, sets Spring Security context, and stores `userId`, `email`, `role` as request attributes — controllers read these via `@RequestAttribute`.

`TokenBlacklistService` persists blacklisted tokens to the `invalidated_tokens` PostgreSQL table (SHA-256 hash) so logout state survives server restarts. An in-memory cache provides O(1) lookup on every request; the cache is pre-populated from DB at startup via `@PostConstruct`.

JWT tokens carry a `type` claim (`"access"` or `"refresh"`). `JwtTokenProvider.validateAsRefreshToken()` checks both signature and type — always use it when accepting a refresh token. The JWT secret is validated at startup (`@PostConstruct`) and must be ≥ 32 characters.

URL-level rules in `SecurityConfig`: `/api/v1/auth/**`, `/api/v1/public/**`, `/api/v1/categories/**` are open; `/api/v1/dev/**` is open only when the `dev` or `docker` profile is active; Swagger paths are open only when `SWAGGER_ENABLED=true`. Everything else requires authentication. Role enforcement is done with `@PreAuthorize` at method level.

`RateLimiterService` provides per-IP sliding-window rate limiting (no external dependencies). `IpUtil` extracts the real client IP and only trusts `X-Forwarded-For` when the connecting IP is in `APP_TRUSTED_PROXY_IPS`.

## Frontend architecture

- **State** — Zustand stores in `src/stores/` (`authStore`, `cartStore`).
- **Data fetching** — React Query (`@tanstack/react-query`) wrapping Axios calls in `src/api/`.
- **API client** — `src/api/client.ts` creates an Axios instance pointed at `VITE_API_BASE_URL`.
- **Routing** — React Router v6; route-level pages in `src/pages/`.
- **3D** — Three.js via `@react-three/fiber` and `@react-three/drei` (used on the landing page).

## Git / commit convention

Branch naming: `feature/<scope>-<desc>` · `bugfix/<scope>-<desc>` · `hotfix/<scope>-<desc>` · `docs/<desc>`

Conventional commits: `feat` · `fix` · `refactor` · `docs` · `test` · `chore`  
Format: `type(scope): short summary` — e.g. `feat(backend): add order cancellation endpoint`

Schema changes require a new Flyway migration (`VN__description.sql`). Never edit existing migration files.

## Dev credentials (local only)

| Email | Password | Role |
|---|---|---|
| `admin@uit.edu.vn` | `Admin123` | ADMIN |
| `org1@uit.edu.vn` | `Org12345` | ORGANIZER |
| `cust1@uit.edu.vn` | `Cust1234` | CUSTOMER |
