# Contributing to UITMerch

Thank you for contributing to UITMerch. This repository contains:
- `backend/`: Spring Boot API (Java 21, PostgreSQL, Flyway, JWT, Supabase Storage)
- `frontend/`: React + TypeScript app (Vite, Tailwind, Zustand, React Query)

## Prerequisites

Install these tools before you start:

- Java `21` (required by `backend/pom.xml`)
- Node.js `18+` (recommended `20 LTS` for Vite 5)
- npm `9+`
- Git
- PostgreSQL (local or managed)

Optional but recommended:
- IntelliJ IDEA / VS Code
- Postman or Bruno for API testing

## Getting Started

### 1) Clone and enter the repository

```bash
git clone <repo-url>
cd UITMerch
```

### 2) Backend setup

```bash
cd backend
copy .env.example .env
```

Fill values in `backend/.env` (database, JWT secret, Supabase keys, mail settings).

Run backend:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

### 3) Frontend setup

```bash
cd ../frontend
npm install
npm run dev
```

Default frontend dev URL: `http://localhost:5173` (Vite will choose another port if occupied).

## Project Structure

```text
UITMerch/
+- backend/                      # Spring Boot backend service
¦  +- src/main/java/com/uitmerch/backend/
¦  ¦  +- auth/                   # Authentication (controller/service/entity/repository)
¦  ¦  +- catalog/                # Merch catalog APIs and business logic
¦  ¦  +- orders/                 # Order lifecycle, checkout, status transitions
¦  ¦  +- organizations/          # Organization management
¦  ¦  +- collections/            # Collections and order success event handling
¦  ¦  +- carts/                  # Cart entities and repositories
¦  ¦  +- admin/                  # Admin-facing APIs/services
¦  ¦  +- common/                 # Shared config, security, exceptions, models
¦  +- src/main/resources/
¦  ¦  +- application.yaml        # Main runtime configuration
¦  ¦  +- db/migration/           # Flyway SQL migrations
¦  +- src/test/                  # Backend tests
¦  +- .env.example               # Backend env template
¦  +- pom.xml                    # Maven dependencies/build config
+- frontend/                     # React frontend app
¦  +- src/
¦  ¦  +- api/                    # API client setup
¦  ¦  +- components/             # UI and page-specific components
¦  ¦  +- pages/                  # Route-level pages
¦  ¦  +- stores/                 # Zustand stores (auth/cart)
¦  ¦  +- mocks/                  # Mock responses and sample data
¦  ¦  +- main.tsx                # App entry
¦  +- public/assets/figma/       # Static design assets
¦  +- .env.example               # Frontend env template
¦  +- package.json               # Frontend scripts and dependencies
+- README.md                     # Root readme (currently minimal)
```

## Git Workflow

This project should follow a `main + develop + short-lived branches` model.

### Branch naming

- `feature/<scope>-<short-description>`
- `bugfix/<scope>-<short-description>`
- `hotfix/<scope>-<short-description>`
- `docs/<short-description>`

Examples:
- `feature/frontend-product-grid`
- `bugfix/backend-order-status-validation`
- `hotfix/auth-jwt-expiration`

### Step-by-step flow

1. Sync base branch.
2. Create your branch.
3. Implement and test locally.
4. Commit with conventional message.
5. Push and open Pull Request.

```bash
# from repository root
git checkout develop
git pull origin develop

git checkout -b feature/frontend-home-hero

# do changes...

git add .
git commit -m "feat(frontend): add home hero CTA animation"
git push -u origin feature/frontend-home-hero
```

If `develop` does not exist yet in your remote workflow, branch from `main` and target `main` in PR.

## Commit Convention

Use Conventional Commit style:

- `feat`: new feature
- `fix`: bug fix
- `refactor`: code restructuring without behavior change
- `docs`: documentation only
- `test`: add/update tests
- `chore`: maintenance/build/config

Format:

```text
type(scope): short summary
```

Examples for this project:

- `feat(frontend): add merch search filters on HomePage`
- `fix(backend): prevent invalid order status transition`
- `refactor(auth): split token parsing from AuthServiceImpl`
- `docs(contributing): document branch and commit workflow`
- `test(orders): add checkout edge-case coverage`
- `chore(frontend): bump vite and typescript`

## Development Workflow

This is the recommended flow for implementing one feature end-to-end.

### Example: add a new backend endpoint and consume it in frontend

1. Add endpoint in backend controller, service interface, and service impl.
2. If schema changes are needed, add a new Flyway migration under `backend/src/main/resources/db/migration`.
3. Start backend and verify endpoint manually.
4. Add frontend API call in `frontend/src/api/client.ts`.
5. Connect UI page/component in `frontend/src/pages` or `frontend/src/components`.
6. Add/update state in `frontend/src/stores` if needed.
7. Run build checks for both backend and frontend.

Backend controller snippet (Spring Boot):

```java
@GetMapping("/api/merch/featured")
public ResponseEntity<ApiResponse<List<MerchItemResponse>>> getFeaturedMerch() {
    return ResponseEntity.ok(ApiResponse.success(merchService.getFeaturedMerch()));
}
```

Frontend API client snippet (Axios):

```ts
import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL
});

export async function getFeaturedMerch() {
  const { data } = await api.get("/merch/featured");
  return data;
}
```

Frontend usage snippet (React Query):

```ts
const { data, isLoading } = useQuery({
  queryKey: ["featured-merch"],
  queryFn: getFeaturedMerch
});
```

## Testing

Backend test framework is available via `spring-boot-starter-test`.

Run backend tests:

```bash
cd backend
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Current test locations include:
- `backend/src/test/java/com/uitmerch/backend/BackendApplicationTests.java`
- `backend/src/test/java/com/uitmerch/backend/orders/service/OrderServiceImplTest.java`

Frontend currently has no configured automated test script in `frontend/package.json`. For now, validate frontend changes with:

```bash
cd frontend
npm run build
npm run dev
```

## Code Style

### Frontend (TypeScript)

Inferred from `frontend/tsconfig.json`:

- `strict: true` (strict type checking)
- `moduleResolution: "Bundler"`
- `jsx: "react-jsx"`
- `noEmit: true` (type-check/build pipeline controls output)
- Keep code compatible with ES2020 target

No ESLint/Prettier config is currently committed in this repo. Keep style consistent with existing files and prefer small, focused PRs.

### Backend (Java)

Inferred from current project structure and Spring conventions:

- Keep layered architecture: `controller -> service -> repository`
- Put shared concerns in `common/`
- Handle schema changes only via Flyway migrations (do not rely on Hibernate auto-DDL)
- Use DTOs for API contracts; avoid exposing JPA entities directly

## Commands Reference

### Frontend (`frontend/package.json`)

| Command | Description |
|---|---|
| `npm run dev` | Start Vite dev server |
| `npm run build` | Type-check (`tsc -b`) and build production bundle |
| `npm run preview` | Preview production build locally |

### Backend (Maven Wrapper)

| Command | Description |
|---|---|
| `./mvnw spring-boot:run` | Run backend application |
| `./mvnw test` | Run backend tests |
| `./mvnw clean package` | Build backend artifact |

On Windows, replace `./mvnw` with `.\mvnw.cmd`.
