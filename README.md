<div align="center">

<img src="frontend/public/assets/figma/logo-title.svg" alt="UITMerch" height="120" />

<p>
  A website for exploring "memory fragments" (merchandise) from the University of Information Technology (UIT).
</p>

</div>

[![Java](https://img.shields.io/badge/Java-21-blue.svg?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-cyan.svg?style=for-the-badge&logo=react)](https://reactjs.org)

## Overview

UITMerch is a modular monolith MVP for selling university merchandise and managing events.

## Audience

- Students
- Organizers (Clubs, Faculty, University admins)

## Key Features

- Public merch catalog with search and pagination
- Organizer workflows: create organizations, publish merch, create events
- Customer flows: cart, wishlist, registered and guest checkout (COD)
- Admin: organization approval and role management

## Tech Stack

- Backend: Spring Boot (Java 21), PostgreSQL, Flyway, JWT auth, Supabase storage integration
- Frontend: React + TypeScript, Vite, Tailwind CSS, Zustand

## Architecture & Docs

- Backend SRS and architecture: [docs/UITMERCH_SRSv2.0.md](docs/UITMERCH_SRSv2.0.md)
- Backend env setup: [backend/ENV_SETUP.md](backend/ENV_SETUP.md)
- Backend build config: [backend/pom.xml](backend/pom.xml)
- Frontend entry point: [frontend/src/main.tsx](frontend/src/main.tsx)
- Frontend scripts: [frontend/package.json](frontend/package.json)
- Contribution guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Agent guidance: [AGENTS.md](AGENTS.md)

## Quickstart

Prerequisites:

- Java 21
- Node.js 18+ and npm 9+
- PostgreSQL (local or managed)

Backend (from repository root):

```powershell
cd backend
copy .env.example .env
./mvnw spring-boot:run
# on Windows PowerShell use: .\mvnw.cmd spring-boot:run
```

Backend (bash):

```bash
cd backend
cp .env.example .env
./mvnw spring-boot:run
```

Frontend (from repository root):

```bash
cd frontend
npm install
npm run dev
```

## API Highlights

- `GET /api/v1/merch` - list/search merch
- `GET /api/v1/organizations` - list organizations/clubs

## Sample Development Credentials

For local development only. Do not use in production.

- Dev user: `dev@uitmerch.local` / `password123`
- Admin user: `admin@uitmerch.local` / `adminpass123`

## Project Structure

- Backend code: [backend/src/main/java](backend/src/main/java)
- Backend migrations: [backend/src/main/resources/db/migration](backend/src/main/resources/db/migration)
- Frontend pages: [frontend/src/pages](frontend/src/pages)
- Frontend components: [frontend/src/components](frontend/src/components)
- Frontend stores: [frontend/src/stores](frontend/src/stores)

## Testing & Build

Backend:

```powershell
cd backend
./mvnw test
./mvnw clean package
```

Frontend:

```bash
cd frontend
npm run build
```

## Deployment

Docker support is planned for later. Current setup uses local Maven and Vite commands.

## Team Members

| Name              | Role               |
| ----------------- | ------------------ |
| Nguyen Quoc Hai   | Project Manager    |
| Ho Nhat Khanh     | Backend Developer  |
| Huynh Tinh Van    | UX/UI Designer     |
| Hoang Khoi Nguyen | Frontend Developer |
| Huynh Nguyen Phu  | Data Analyst       |

## Contributing

Please follow the process in [CONTRIBUTING.md](CONTRIBUTING.md). Keep changes scoped (backend vs frontend) and include Flyway SQL when changing DB schema.

## License

This project is released under the terms of the repository license. See [LICENSE](LICENSE).

## Project Note

This project was created to commemorate the 20th anniversary of the University of Information Technology (UIT) and is not intended for commercial purposes in any form.
