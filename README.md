<div align="center">

<img src="frontend/public/assets/figma/logo-title.svg" alt="UITMerch" height="120" />

<p>
  A platform for discovering and collecting merchandise from the University of Information Technology (UIT) — built for students, clubs, and departments.
</p>

[![Live Demo](https://img.shields.io/badge/Live%20Demo-uitmerch--fe.vercel.app-brightgreen?style=for-the-badge&logo=vercel)](https://uitmerch-fe.vercel.app)
[![Java](https://img.shields.io/badge/Java-21-blue.svg?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-cyan.svg?style=for-the-badge&logo=react)](https://reactjs.org)
[![Vercel](https://img.shields.io/badge/Frontend-Vercel-black?style=for-the-badge&logo=vercel)](https://uitmerch-fe.vercel.app)

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

**Frontend:** [https://uitmerch-fe.vercel.app](https://uitmerch-fe.vercel.app)

> The frontend is deployed on **Vercel**. The backend (Spring Boot) is self-hosted and may require a separate running instance for full API functionality.

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
| `/event` | **Sự Kiện & Hoạt Động** (Events) | Event listing with status badges (Sắp diễn ra / Đang diễn ra / Đã kết thúc), filter by Newest or Upcoming |
| `/auth` | **Tài Khoản** (Account) | Login / Register portal with role selection (Customer / Organizer) and OTP email verification |

### Customer (login required)
- Cart and wishlist management
- Registered account checkout and guest checkout (Cash on Delivery)
- Order history and status tracking

### Organizer (login + approved organization)
- Create and manage an organization profile (subject to admin approval)
- Publish, edit, and remove merchandise with multi-image gallery support
- Create and manage events linked to their organization

### Admin
- Approve or reject organization registration requests
- Assign and revoke user roles
- Platform-wide content oversight

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
| Spring Security + JWT | Authentication and role-based access control |
| PostgreSQL + Flyway | Relational database with versioned migrations |
| Supabase Storage | Image hosting for merch and organization logos |
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

- Backend SRS and architecture: [docs/UITMERCH_SRSv2.0.md](docs/UITMERCH_SRSv2.0.md)
- Backend env setup: [backend/ENV_SETUP.md](backend/ENV_SETUP.md)
- Backend build config: [backend/pom.xml](backend/pom.xml)
- Frontend entry point: [frontend/src/main.tsx](frontend/src/main.tsx)
- Frontend scripts: [frontend/package.json](frontend/package.json)
- Contribution guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Agent guidance: [AGENTS.md](AGENTS.md)

---

## Quickstart

**Prerequisites:**
- Java 21
- Node.js 18+ and npm 9+
- PostgreSQL (local or managed)
- Docker (optional, recommended for backend)

### Backend — Docker (recommended)

```bash
cd backend
docker compose up --build
```

- API → `http://localhost:8080`
- Swagger UI → `http://localhost:8080/swagger-ui.html`
- No `.env` file required — sample data is seeded automatically.

### Backend — Dev profile (local, H2 in-memory)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Windows: .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

No env vars required. Schema is generated by Hibernate and data resets on every restart.

### Backend — Production (PostgreSQL + Supabase)

```bash
cd backend
cp .env.example .env   # fill in values per backend/ENV_SETUP.md
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
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
| `GET/POST` | `/api/v1/customer/cart` | Customer | Cart management |
| `GET/POST` | `/api/v1/customer/orders` | Customer | Order management |
| `CRUD` | `/api/v1/organizations/merchs` | Organizer | Merch management |
| `CRUD` | `/api/v1/organizations/events` | Organizer | Event management |
| `GET/PATCH` | `/api/v1/admin/organizations` | Admin | Organization approval |

Full API reference: [backend/README.md](backend/README.md)

---

## Sample Development Credentials

> For local development only. **Do not use in production.**

**Docker / dev profile (auto-seeded):**

| Email | Password | Role |
|---|---|---|
| `admin@uit.edu.vn` | `Admin123` | ADMIN |
| `org1@uit.edu.vn` | `Org12345` | ORGANIZER (ACTIVE org) |
| `org2@uit.edu.vn` | `Org12345` | ORGANIZER (PENDING org) |
| `cust1@uit.edu.vn` | `Cust1234` | CUSTOMER |
| `cust2@uit.edu.vn` | `Cust1234` | CUSTOMER |

**PostgreSQL real seed (V12–V15 migrations):**

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
│   └── src/main/java/              # Domain modules (auth, merch, org, event, order)
│   └── src/main/resources/
│       └── db/migration/           # Flyway SQL migrations
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

```powershell
cd backend
./mvnw test
./mvnw clean package
```

**Frontend:**

```bash
cd frontend
npm run build
```

---

## Deployment

**Frontend** is deployed on **Vercel**: [https://uitmerch-fe.vercel.app](https://uitmerch-fe.vercel.app)

- The `frontend/vercel.json` catch-all rewrite ensures React Router handles all client-side routes correctly on page refresh.
- No additional build configuration is needed — Vite outputs to `dist/` and Vercel serves it automatically.

**Backend** requires a Java 21 runtime with PostgreSQL and (optionally) a Supabase project for image storage. Refer to [backend/ENV_SETUP.md](backend/ENV_SETUP.md) for full configuration.

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
