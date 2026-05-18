# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Spring Boot 3.3.5 · Java 21 · PostgreSQL · Flyway (V1–V24) · JWT (JJWT 0.12.3) · springdoc-openapi 2.6.0 · Maven · logstash-logback-encoder 7.4

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
- `traceId` comes from `TraceIdInterceptor` (MDC) — never pass manually.
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
- `@Valid` is required on every `@RequestBody` parameter — bean validation constraints are silently skipped without it.

## Module structure (per domain)

```
entity/ → repository/ → dto/ → service/ → controller/
```

## Swagger

Tag groups (order defined in `common/config/OpenApiConfig.java`):

**Admin → Auth → Public → Organizer → Customer**

Every controller must have `@Tag`, `@Operation`, `@ApiResponse` on every endpoint, and `@SecurityRequirement(name = "bearerAuth")` on protected controllers. The tag `name` must exactly match one of the five groups above so the endpoint appears in the right Swagger section.

Swagger is disabled in production by default (`springdoc.swagger-ui.enabled: ${SWAGGER_ENABLED:false}`). The security rule in `SecurityConfig` mirrors this flag — swagger paths are only opened when `SWAGGER_ENABLED=true`.

## Profiles

- **`dev`** — H2 in-memory, Flyway disabled, Hibernate `create-drop`, `DevEmailService` (logs OTP), `DevStorageService` (placeholder URL), `DevOtpController` active. No env vars needed.
- **`docker`** — Same stubs as `dev` but uses real PostgreSQL (from Docker Compose). `DevOtpController` active.
- **default** — PostgreSQL + `SupabaseStorageServiceImpl` + `JavaMailEmailService` (async). Requires `.env`.

## Security

### JWT tokens

- Access and refresh tokens are distinguished by a `type` claim (`"access"` / `"refresh"`).
- `JwtTokenProvider.validateAsRefreshToken()` checks both signature and type — use it in any flow that accepts a refresh token.
- The JWT secret must be ≥ 32 characters. A `@PostConstruct` in `JwtTokenProvider` throws `IllegalStateException` at startup if it is too short.
- Blacklisted tokens are stored in the `invalidated_tokens` table (SHA-256 hash, `expires_at`). `TokenBlacklistService` caches them in memory and pre-loads from DB at startup so logout state survives restarts.

### Rate limiting

`RateLimiterService` implements a per-key sliding-window counter (`ConcurrentHashMap<String, Deque<Instant>>`). Apply it at the Controller level using `IpUtil.extractClientIp(HttpServletRequest)` as the key suffix. Only trust `X-Forwarded-For` when `APP_TRUSTED_PROXY_IPS` is configured and the connecting IP is in the list.

Current limits: login 10/15 min · register/resend-OTP/forgot-password 5/hr · guest checkout 20/hr.

### Dev endpoint

`/api/v1/dev/**` is only opened in `SecurityConfig` when the `dev` or `docker` profile is active (`Environment.acceptsProfiles`). `DevOtpController` itself is also `@Profile("dev | docker")`.

## Schema migrations

Flyway runs on startup (default/docker profiles). Migration files: `src/main/resources/db/migration/VN__description.sql`.

Never edit existing migration files. Always add a new `VN+1__description.sql` for schema changes.

Current highest: **V28** (`notifications` table).

| Migration | Contents |
|---|---|
| V25 | Rename `order_status` enum values: `READY_FOR_PICKUP → READY`, `SUCCESS → COMPLETED` |
| V26 | Add cancel fields to `orders`: `cancelled_by`, `cancel_reason`, `cancel_reason_note`, `cancelled_at` |
| V27 | `pickup_schedules` table + FK from `orders.pickup_schedule_id` |
| V28 | `notifications` table for in-app CUSTOMER notifications |

## Storage

`StorageService` interface — two implementations selected by `@Profile`:

- `DevStorageService` — returns a fake URL, no-op delete. Active on `dev` and `docker`.
- `SupabaseStorageServiceImpl` — S3-compatible Supabase Storage. Active on default.

Never persist images as Base64 or BLOB in the database. Maximum 10 images per merch item (`@Size(max=10)` on `imageUrls`). Each URL must be a valid HTTP/HTTPS URL (`@URL` constraint).

## Email

`EmailService` has three methods: `sendOtp`, `sendPasswordReset`, `sendOrderStatusUpdate`.

- `DevEmailService` — logs to console; no-op. Active on `dev` and `docker`.
- `JavaMailEmailService` — sends via SMTP. All three methods are `@Async` — they never block the calling thread. Email failures are caught and logged as WARN; they never propagate to the caller.

`@EnableAsync` is on `BackendApplication`.

## Caching

`@EnableCaching` is on `BackendApplication`. Spring's default `ConcurrentMapCache` is used (no TTL — cache lives for the JVM lifetime unless explicitly evicted).

| Cache name | What | Eviction |
|---|---|---|
| `categories` | `CategoryRepository.findAll()` result | `@CacheEvict` on all `CategoryRepository` write methods |
| `popular-merch` | Top-10 popularity list | `@CacheEvict` on `MerchService.createMerch` and `updateMerch` |

`getPopularMerch` caps candidates at 500 most-recent published items to avoid a full-table scan on large catalogs.

## @Modifying queries

All `@Modifying` JPQL queries in `MerchItemRepository` use `clearAutomatically = true`. Without this, Hibernate's L1 (first-level) cache returns stale entity state after a bulk UPDATE — `findById` would return the pre-update value within the same transaction. Always add `clearAutomatically = true` to any new `@Modifying` query that updates entity state.

## Order stock deduction

Stock is deducted with an atomic SQL update:

```sql
UPDATE merch_items SET stock = stock - :qty WHERE id = :id AND stock >= :qty
```

This returns 1 on success or 0 if stock was already insufficient (concurrent order won the race). `OrderService` checks the return value and throws `ValidationException` on 0. Never read stock then write it in two steps — this creates a race condition.

When an order is cancelled (`PENDING → CANCELLED` or `CONFIRMED → CANCELLED`), `restoreStock` is called for each order item.

## Logging

Production logging uses `LogstashEncoder` (logstash-logback-encoder 7.4) outputting JSON to stdout. Every log line includes `traceId` from MDC (set by `TraceIdInterceptor`). `RequestLoggingInterceptor` logs `METHOD URI → STATUS (Xms) [traceId]` for every `/api/**` request.

Dev/docker profiles use human-readable coloured output with `DEBUG` for `com.uitmerch`.

## Validation constraints on DTOs

All request DTOs must have:
- `@NotBlank` / `@NotNull` on required fields
- `@Size(max=N)` on all string fields (prevents unbounded DB writes)
- `@URL` on any URL field
- Container-element constraints are supported: `List<@URL String> imageUrls`

The controller method must have `@Valid` on `@RequestBody` or constraints are silently ignored.

## Testing

Tests run with `./mvnw test`. All 172 tests use `@ActiveProfiles("dev")` — H2 in-memory, no PostgreSQL or env vars required.

### @DataJpaTest notes

- `@Modifying` queries need `clearAutomatically = true` to avoid reading stale L1-cached entities in the same transaction.
- Concurrent tests must commit test data before spawning threads. Use `TransactionTemplate` to explicitly commit in a separate transaction; `@Transactional(propagation = NOT_SUPPORTED)` alone is insufficient because `@BeforeEach` data is not visible across connections without a commit.
- If dotenv fails to parse `.env` in the test context, Surefire passes `dotenv.ignoreIfMalformed=true` and `dotenv.ignoreIfMissing=true` as JVM system properties (configured in `pom.xml`).
