# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Spring Boot 3.3.5 · Java 21 · PostgreSQL · Flyway · JWT (JJWT 0.12.3) · springdoc-openapi 2.6.0 · Maven

## Package root

`com.uitmerch.backend`

## Exception handling

All exception classes live in `common/exception/`. Read them before writing service code.

| Class | HTTP | When to use |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found by ID |
| `ConflictException` | 409 | Duplicate resource or violated business rule |
| `ForbiddenException` | 403 | Authenticated but not permitted |
| `AuthenticationException` | 401 | Bad credentials / invalid token |
| `ValidationException` | 400 | Semantic validation failure |
| `InvalidOtpException` | 400 | Wrong or expired OTP |
| `UnverifiedEmailException` | 403 | Email not verified yet |
| `UserAlreadyExistsException` | 409 | Registration with existing email |
| `StorageException` | 500 | File upload/delete failure |

All extend `AppException(message, httpStatus, errorCode)`. Add a new class only if none of the above fits semantically — new classes must be handled in `GlobalExceptionHandler`.

Throw only from Service — never from Controller.

## Response shape

Every response uses `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Human-readable string",
  "data": {},
  "meta": { "page": 0, "size": 10, "total": 100 },
  "traceId": "uuid"
}
```

- `meta` is `null` for non-paginated responses.
- `traceId` is auto-generated — never pass manually.
- Use the static factory methods: `ApiResponse.success(message, data)` / `ApiResponse.success(message, data, meta)` / `ApiResponse.error(message)`.

## HTTP status semantics

| Status | When |
|---|---|
| 200 OK | Successful GET, PATCH |
| 201 Created | Successful POST that creates a resource |
| 204 No Content | Successful DELETE |
| 400 Bad Request | Validation failure, malformed request |
| 401 Unauthorized | Missing or invalid JWT |
| 403 Forbidden | Authenticated but not allowed |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Duplicate resource, business rule violation |
| 422 Unprocessable | Semantically invalid (e.g. insufficient stock) |

`@Valid` failures return field-level errors in `data`:

```json
{ "success": false, "message": "Validation failed", "data": { "email": "Email must be valid" } }
```

## Conventions

- Zero business logic in Controller.
- `@PreAuthorize("hasRole('...')")` at method level — not in `SecurityConfig`.
- UUID PK with `@UuidGenerator`.
- `@Enumerated(EnumType.STRING)` for all enums. Shared enums live in `common/model/`.
- `@CreationTimestamp` / `@UpdateTimestamp` for audit fields.
- No bidirectional JPA relationships.
- All endpoints prefixed `/api/v1/`.
- Paginated endpoints accept `?page=0&size=10&sort=createdAt,desc`. Return `PaginationMeta.from(page)` in the `meta` field.
- Controllers read `userId`/`email`/`role` from `@RequestAttribute` — set by `JwtAuthenticationFilter`.

## Module structure (per domain)

```
entity/ → repository/ → dto/ → service/ → controller/
```

## Swagger

Tag groups (order defined in `common/config/OpenApiConfig.java`):

**Admin → Auth → Public → Organizer → Customer**

Every controller must have `@Tag`, `@Operation`, `@ApiResponse` on every endpoint, and `@SecurityRequirement(name = "bearerAuth")` on protected controllers. The tag `name` must exactly match one of the five groups above so the endpoint appears in the right Swagger section.

## Profiles

- **`dev`** — H2 in-memory, Flyway disabled, Hibernate `create-drop`, `DevEmailService` (no-op), `DevStorageService` (no-op). No env vars needed. `DevDataInitializer` seeds data on startup.
- **`docker`** — Same stubs as `dev` but uses real PostgreSQL (from Docker Compose). Data seeded via `DevDataInitializer`.
- **default** — PostgreSQL + `SupabaseStorageServiceImpl` + `JavaMailEmailService`. Requires `.env`.

## Schema migrations

Flyway runs on startup (default/docker profiles). Migration files: `src/main/resources/db/migration/VN__description.sql`.

Never edit existing migration files. Always add a new `VN+1__description.sql` for schema changes.

## Storage

`StorageService` interface — two implementations selected by `@Profile`:

- `DevStorageService` — returns a fake URL, no-op delete. Active on `dev` and `docker`.
- `SupabaseStorageServiceImpl` — S3-compatible Supabase Storage. Active on default.

Never persist images as Base64 or BLOB in the database.
