<div align="center">

<img src="frontend/public/assets/figma/logo-title.svg" alt="UITMerch" height="120" />

<p>
  A platform for discovering and collecting merchandise from the University of Information Technology (UIT) — built for students, clubs, and departments.
</p>

[![Live Demo](https://img.shields.io/badge/Live%20Demo-uitmerch.vercel.app-brightgreen?style=for-the-badge&logo=vercel)](https://uitmerch.vercel.app)
[![Java](https://img.shields.io/badge/Java-21-blue.svg?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-cyan.svg?style=for-the-badge&logo=react)](https://reactjs.org)

</div>

---

## ⚠️ DISCLAIMER

> **This project is developed strictly for academic study and research purposes.**

All data, content, merchandise information, pricing, images, organization names, event details, and any other information displayed or used within this project — including data stored in databases, seed files, and API responses — are **fictional, illustrative, or sourced solely for educational demonstration**.

**The following restrictions apply without exception:**

- This project is **NOT** intended for commercial use, resale, market deployment, or any form of real-world transaction.
- No actual products are being sold, advertised, or offered for purchase through this platform.
- No real financial transactions are processed. Any payment flows present are mock/simulated implementations for learning purposes only.
- Organization names, logos, and institutional references (e.g., University of Information Technology — UIT) are used **solely to provide realistic academic context** and do not represent official endorsement, affiliation, or authorization by those institutions.
- Any resemblance to real products, pricing, or commercial offerings is coincidental and unintentional.

**Authorized use:** This codebase may be reviewed, studied, forked, and modified for non-commercial educational purposes only, in accordance with the repository license.

If you have concerns about specific content or data used in this project, please contact the project team via the repository's issue tracker.

---

## 🌐 Live Demo

**Frontend:** [https://uitmerch.vercel.app](https://uitmerch.vercel.app)

---

## Overview

**UITMerch** is a full-stack web platform built to simulate a university merchandise store for the **University of Information Technology (UIT)**. It serves as a practical study of modern software engineering — covering REST API design, role-based access control, database migrations, session-based caching, and a reactive SPA frontend — all within a single cohesive product.

The platform is built around three user types:

- **Customers** (students, alumni, UIT fans) — browse and purchase official merchandise from clubs, faculties, and university events.
- **Organizers** (clubs, departments, event committees) — manage their own storefronts: publishing products, setting prices, and running events linked to their organization.
- **Administrators** — approve organization registrations, manage user roles, and oversee platform content.

The backend is a **modular monolith** built on Spring Boot, cleanly separating domain boundaries (auth, merch, organization, event, order) without microservice overhead. The frontend is a **single-page application** built with React + TypeScript, featuring a glassmorphism UI design system, scroll-snap homepage, session-based caching for instant page navigation, and a fully responsive layout.

> This project was created to commemorate the 20th anniversary of UIT and is used purely for academic research and study. See the [DISCLAIMER](#️-disclaimer) above.

---

## Pages & Features

### Public (no login required)

| Route | Page | Description |
|---|---|---|
| `/` | **Trang chủ** (Home) | Scroll-snap homepage with featured merch slider, organization grid, and campus info section |
| `/merch` | **Kho Vật Phẩm** (Merch Store) | Full catalog with keyword search, category filters (Đồ lưu niệm, Trang phục, Đồ dùng), sorting, and pagination (16 items/page) |
| `/organization` | **Tổ Chức** (Organizations) | Grid of 30+ clubs and faculties with search-all and sort |
| `/events` | **Sự Kiện & Hoạt Động** (Events) | Event listing with status badges (Sắp diễn ra / Đang diễn ra / Đã kết thúc), filter by Newest or Upcoming |
| `/auth` | **Tài Khoản** (Account) | Login / Register portal with role selection (Customer / Organizer), OTP email verification, and forgot-password / reset-password flow |

### Customer (login required)
- Cart and wishlist management
- Registered account checkout and guest checkout (cash on delivery)
- Order history and status tracking
- Cancel PENDING orders with a reason (predefined options + free-text note)
- In-app notifications (SSE real-time) — bell icon in nav; ORDER_PLACED confirmation on checkout, notified when order reaches READY for campus pickup
- Campus pickup flow: orders progress PENDING → CONFIRMED → READY → COMPLETED; organizer creates pickup schedule slots and marks orders COMPLETED at check-in

### Organizer (login + approved organization)
- Create and manage an organization profile (subject to admin approval)
- Publish, edit, and remove merchandise with multi-image gallery support
- Create and manage events linked to their organization
- Confirm and manage orders: PENDING → CONFIRMED → READY (via pickup schedule) → COMPLETED (campus check-in)
- Create pickup schedule slots that batch-transition CONFIRMED orders to READY and notify customers
- Cancel PENDING or CONFIRMED orders with a reason
- In-app notifications (SSE real-time) — notified on new orders and customer cancellations; persisted across page refreshes

### Admin
- Approve or reject organization registration requests
- Assign and revoke user roles
- Platform-wide content oversight

### Integrated features

The backend and frontend implement the following features. Customer settings, guest tracking, campaign discovery and organizer tabs are documented in the [frontend reference](frontend/README.md). Local changes are distinct from the currently deployed version; see [combined validation](docs/reviews/2026-10-02-frontend-validation.md).

| Feature | Behavior |
|---|---|
| Restock subscriptions | Customers subscribe to products and receive in-app alerts, with optional email, when stock changes from zero to positive |
| QR pickup and order history | One-time pickup credentials for customers and guests, organizer verification/check-in, and an ownership-protected order audit trail |
| Organization following | Customers choose merchandise/event alerts and receive notifications on an organization's first publication of each item |
| Organizer analytics | Organization-scoped order, stock, product sales, daily activity, and pickup workload reports |
| Preorder campaigns | Organizers set variants, a minimum quantity, and a deadline; customer reservations deduct stock and become fulfillable when the campaign succeeds |

See the [backend API reference](backend/README.md#new-feature-apis) and [implementation notes](docs/reviews/2026-10-02-backend-features.md) for request bodies and workflow details.

---

## Audience

| Role | Description |
|---|---|
| **Student / Customer** | Browse the full merch catalog, search and filter, add to cart, place orders (registered or guest COD checkout) |
| **Organizer** | Register an organization, publish merchandise, manage events and their associated products |
| **Administrator** | Approve organizations, manage user roles, oversee platform-wide content |

---

## Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| Java 21 + Spring Boot 3.3 | Core API framework (modular monolith) |
| Spring Security + JWT | Persisted authentication sessions, refresh rotation, revocation, and role/ownership checks |
| PostgreSQL + pgvector + Flyway | Relational database and migrations V1–V43; pgvector supports AI visual search |
| Supabase Storage | Image hosting for merch and organization logos |
| Database outbox + SMTP | Durable email and announcement delivery with retries |
| Swagger / OpenAPI | Auto-generated API documentation |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 + TypeScript | Component-based SPA |
| Vite | Build tool and dev server |
| Tailwind CSS | Utility-first styling with custom design tokens |
| React Router v6 | Client-side routing |
| Zustand | Lightweight global state management |
| sessionStorage cache | In-session data caching for instant page navigation |
| Vercel | Frontend hosting and deployment |

---

## Architecture & Docs

- Backend SRS and architecture: [backend/docs/UITMERCH_SRSv2.0.md](backend/docs/UITMERCH_SRSv2.0.md)
- Backend setup and API reference: [backend/README.md](backend/README.md)
- Backend feature plan: [docs/reviews/2026-10-01-backend-plan.md](docs/reviews/2026-10-01-backend-plan.md)
- Implemented backend features: [docs/reviews/2026-10-02-backend-features.md](docs/reviews/2026-10-02-backend-features.md)
- Recorded backend validation: [docs/reviews/2026-10-02-backend-feature-validation.json](docs/reviews/2026-10-02-backend-feature-validation.json)
- Backend environment reference: [environment variables](backend/README.md#environment-variables)
- Backend build config: [backend/pom.xml](backend/pom.xml)
- Frontend entry point: [frontend/src/main.tsx](frontend/src/main.tsx)
- Frontend scripts: [frontend/package.json](frontend/package.json)
- Contribution guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Agent guidance: [AGENTS.md](AGENTS.md)

---

## Quickstart

**Prerequisites:**
- Java 21
- Node.js 22 LTS and npm
- PostgreSQL with the `vector` extension available (for PostgreSQL runs)
- Docker (optional for the application; required for the isolated PostgreSQL test harness)

### Backend — Docker (configured PostgreSQL)

```bash
cd backend
cp .env.example .env
# Fill in database, JWT, mail, storage, and CORS values.
# Set SWAGGER_ENABLED=true in .env to use local Swagger.
docker compose up --build
```

- API → `http://localhost:8080`
- Swagger UI → `http://localhost:8080/swagger-ui.html` when enabled
- Compose runs the backend container and connects to the PostgreSQL database configured in `.env`; it does not create a database container. Flyway applies migrations on startup.

### Backend — Dev profile (local, H2 in-memory)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Windows: .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

No external database or `.env` is required. Hibernate generates the H2 schema and sample data resets on every restart. Mail and storage use development substitutes. Flyway is disabled in this profile.

Swagger UI: `http://localhost:8080/swagger-ui.html` (enabled automatically in `dev`).

### Backend — Default profile (PostgreSQL + Supabase)

```bash
cd backend
cp .env.example .env   # fill in values per backend/README.md
./mvnw spring-boot:run
```

The default profile uses PostgreSQL, real SMTP, and Supabase Storage. The database must support pgvector and allow Flyway to create the `vector` extension, including when AI API keys are omitted. Swagger defaults to disabled; set `SWAGGER_ENABLED=true` for local API exploration.

### Frontend

```bash
cd frontend
npm ci
cp .env.example .env.local
npm run dev        # development server at http://localhost:5173
npm run build      # production build to dist/
```

---

## API Highlights

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Public | Login and receive JWT |
| `POST` | `/api/v1/auth/register` | Public | Register new account |
| `GET` | `/api/v1/public/merch` | Public | List/search merch with filters |
| `GET` | `/api/v1/public/organizations` | Public | List active organizations |
| `GET` | `/api/v1/public/events` | Public | List events |
| `GET` | `/api/v1/categories` | Public | List all categories |
| `POST` | `/api/v1/customer/cart/checkout` | Customer | Checkout cart, grouped by organization |
| `POST` | `/api/v1/customer/orders/instant` | Customer | Place a single-item order |
| `PATCH` | `/api/v1/customer/orders/{id}/cancel` | Customer | Cancel own PENDING order and restore stock |
| `GET` | `/api/v1/customer/notifications/stream` | Customer | Real-time notification stream |
| `GET` | `/api/v1/organizer/notifications` | Organizer | Own persisted notifications |
| `CRUD` | `/api/v1/organizations/{orgId}/merchs` | Organizer | Merch management |
| `CRUD` | `/api/v1/organizations/{orgId}/events` | Organizer | Event management |
| `GET` | `/api/v1/organizations/{orgId}/orders` | Organizer | Own organization orders |
| `POST` | `/api/v1/customer/restock-subscriptions` | Customer | Subscribe to restock alerts |
| `POST` | `/api/v1/customer/orders/{orderId}/pickup-token` | Customer | Issue a pickup credential for an owned READY order |
| `POST` | `/api/v1/organizations/{orgId}/orders/pickup/checkin` | Organizer | Consume a pickup credential and complete its order |
| `POST` | `/api/v1/customer/following/{orgId}` | Customer | Follow an organization with notification preferences |
| `GET` | `/api/v1/organizations/{orgId}/analytics` | Organizer | Report for an owned organization, filtered by date |
| `GET` | `/api/v1/public/campaigns` | Public | Discover preorder campaigns |
| `POST` | `/api/v1/customer/campaigns/{id}/reservations` | Customer | Reserve a campaign variant using a unique request ID |
| `PATCH` | `/api/v1/admin/organizations/{id}/status` | Admin | Approve or deactivate an organization |

Full API reference: [backend/README.md](backend/README.md)

---

## Sample Development Credentials

For the updated following, stock reminders, free products, order statuses, and preorder collections, use the opt-in [feature demo guide](docs/reviews/2026-10-03-app-management-refactor.md). Run `python3 backend/scripts/seed-feature-demo.py --database configured` from root. The generated password for `demo.customer@uitmerch.test`, `demo.organizer@uitmerch.test`, and `demo.admin@uitmerch.test` stays in private, ignored `backend/.demo-credentials.json`.

> For local development only. **Do not use in production.**

**Dev profile (auto-seeded):**

| Email | Password | Role |
|---|---|---|
| `admin@uit.edu.vn` | `Admin123` | ADMIN |
| `org1@uit.edu.vn` | `Org12345` | ORGANIZER (ACTIVE org) |
| `org2@uit.edu.vn` | `Org12345` | ORGANIZER (PENDING org) |
| `cust1@uit.edu.vn` | `Cust1234` | CUSTOMER |
| `cust2@uit.edu.vn` | `Cust1234` | CUSTOMER |

**PostgreSQL demonstration seed (V12–V16 migrations, including default Compose startup):**

| Email | Password | Role |
|---|---|---|
| `admin@uitmerch.edu.vn` | `UIT@2025` | ADMIN |
| `cs.khmt@uit.edu.vn` | `UIT@2025` | ORGANIZER |
| `nguyen.van.an@student.uit.edu.vn` | `UIT@2025` | CUSTOMER |

---

## Project Structure

```
UITMerch/
├── backend/                        # Spring Boot application
│   ├── src/main/java/              # Core domains + restock, pickup, following, analytics, campaign
│   ├── src/main/resources/
│   │   └── db/migration/           # Flyway migrations V1–V43
│   ├── src/test/                   # Unit, H2, PostgreSQL, migration and concurrency tests
│   └── scripts/test-postgres.sh    # Isolated full-suite test runner
├── frontend/                       # React + TypeScript SPA
│   └── src/
│       ├── api/                    # API client functions
│       ├── components/             # Reusable UI components
│       ├── lib/                    # Utilities (sessionCache, etc.)
│       ├── pages/                  # Route-level page components
│       ├── stores/                 # Zustand global state
│       └── types/                  # Shared TypeScript types
│   └── vercel.json                 # SPA routing config for Vercel
└── docs/                           # Architecture and SRS documentation
```

---

## Testing & Build

**Backend:**

```bash
cd backend
./mvnw test
./mvnw clean package
```

PostgreSQL-dependent suites are skipped without a test database. To run the complete backend suite using a disposable PostgreSQL container, from the repository root:

```bash
backend/scripts/test-postgres.sh -q
```

The harness requires Bash, Docker, and Java 21, copies the backend into a temporary directory, and prints the retained report location. It creates and removes its own test database container. The [recorded validation on 2 October 2026](docs/reviews/2026-10-02-backend-feature-validation.json) contains **253 tests across 27 classes, with no failures, errors, or skips**. The [earlier combined validation](docs/reviews/2026-10-02-frontend-validation.md) includes the campaign context APIs: **257 backend tests, 40 frontend unit/component tests and 17 browser scenarios**. The [backend environment fixes](docs/reviews/2026-10-02-backend-environment-fixes.md) restore the deployed migration lineage through V43 and verify runtime environment bindings. The [3 October Docker validation](docs/reviews/2026-10-03-docker-validation.md) passes **265 backend tests across 30 classes, 40 frontend tests and 45 browser scenarios**, verifies a healthy rebuilt container and fixes public campaign pagination.

The [latest interface and management validation](docs/reviews/2026-10-03-app-management-refactor.md) verifies **269 backend tests, 42 frontend tests, 73 browser cases across the full and expanded management runs**, plus 15 live API endpoints and 9 frontend screens against a healthy rebuilt Docker backend.

**Frontend:**

```bash
cd frontend
npm test
npx playwright install --with-deps chromium
npm run test:e2e
npm run build
```

---

## Deployment

**Frontend** is deployed on **Vercel**: [https://uitmerch.vercel.app](https://uitmerch.vercel.app)

- The `frontend/vercel.json` catch-all rewrite ensures React Router handles all client-side routes correctly on page refresh.
- Configure root `frontend`, install `npm ci`, build `npm run build`, output `dist`, and environment-specific `VITE_API_BASE_URL`.
- Deploy backend V43 and the campaign context endpoints before this frontend. List exact production/preview origins in backend CORS.
- GitHub CI runs isolated PostgreSQL backend tests plus frontend unit/browser/build checks before triggering Render on `main`. Vercel Git deployments are independent; its private settings must be checked separately.
- See [combined validation and deploy findings](docs/reviews/2026-10-02-frontend-validation.md) for the current production mismatch and local results.

**Backend** requires a Java 21 runtime, PostgreSQL with pgvector, SMTP configuration, and Supabase Storage configuration for the default profile. Refer to the [backend environment reference](backend/README.md#environment-variables) for configuration.

---

## Team Members

| Name | Role |
|---|---|
| Nguyen Quoc Hai | Project Manager |
| Ho Nhat Khanh | Backend Developer |
| Huynh Tinh Van | UX/UI Designer |
| Hoang Khoi Nguyen | Frontend Developer |
| Huynh Nguyen Phu | Data Analyst |

---

## Contributing

Please follow the process in [CONTRIBUTING.md](CONTRIBUTING.md). Keep changes scoped (backend vs frontend) and include Flyway SQL migrations when changing the database schema.

---

## License

This project is released under the terms of the repository license. See [LICENSE](LICENSE).

---

## Project Note

This project was created to commemorate the **20th anniversary of the University of Information Technology (UIT)** and is not intended for commercial purposes in any form.
