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

`auth` · `user` · `organization` · `merch` · `cart` · `order` · `wishlist` · `event` · `admin` · `common`

Each follows: `entity/ → repository/ → dto/ → service/ → controller/`

### Dual-implementation services

Both swap automatically via `@Profile`:

- **`StorageService`** — `DevStorageService` (dev/docker, no-op) vs `SupabaseStorageServiceImpl` (production S3-compatible). Never store images as Base64/BLOB in the DB.
- **`EmailService`** — `DevEmailService` (dev/docker, no-op) vs `JavaMailEmailService` (production SMTP).

### Security

`JwtAuthenticationFilter` validates the `Authorization: Bearer <token>` header on every request, sets Spring Security context, and stores `userId`, `email`, `role` as request attributes — controllers read these via `@RequestAttribute`.

`TokenBlacklistService` is an in-memory blacklist (ConcurrentHashMap) for logout. It is not cluster-safe — replace with Redis for multi-instance deployments.

URL-level rules in `SecurityConfig`: `/api/v1/auth/**`, `/api/v1/public/**`, `/api/v1/categories/**` are open; everything else requires authentication. Role enforcement is done with `@PreAuthorize` at method level.

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
