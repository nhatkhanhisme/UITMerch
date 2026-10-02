# UITMerch Backend

Spring Boot 3.3.5 / Java 21 REST API for the UIT merchandise platform.
Supports Customers, Organizers, and Admins, with restock alerts, QR pickup, organization following, analytics, and preorder campaigns.

See the [root README](../README.md) for the product and frontend overview, and [feature implementation notes](../docs/reviews/2026-10-02-backend-features.md) for detailed backend workflows.

---

## Quick Start

Commands below run from `backend/`. Java 21 is required for local Maven commands; the Docker image supplies its own Java runtime.

### Option 1 — Docker (configured PostgreSQL)

```bash
cp .env.example .env
# Fill in database, JWT, mail, storage, and CORS values.
# Set SWAGGER_ENABLED=true in .env for local Swagger access.
docker compose up --build
```

- API → `http://localhost:8080`
- Swagger UI → `http://localhost:8080/swagger-ui.html` when `SWAGGER_ENABLED=true`
- OpenAPI JSON → `http://localhost:8080/v3/api-docs` when enabled

Compose runs one backend service, requires `.env`, and connects to the database specified by `SPRING_DATASOURCE_URL`. It does not provision PostgreSQL. The supplied Dockerfile runs the default Spring profile, using Flyway, real SMTP, and Supabase Storage. Building a Docker container does not activate the Spring `docker` profile.

```bash
docker compose up           # subsequent starts (faster, uses cached image)
docker compose down         # stop the backend; the external database is retained
docker compose logs -f backend
```

### Option 2 — Dev profile (local, H2 in-memory)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Windows: .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

No external database or `.env` is required. Hibernate generates the H2 schema and data resets on every restart. Flyway is disabled; mail and storage use development substitutes. Use PostgreSQL tests to validate migrations and PostgreSQL locking behavior.

- API → `http://localhost:8080`
- Swagger UI → `http://localhost:8080/swagger-ui.html` (automatically enabled)

OTPs are not emailed — fetch them directly:
```
GET http://localhost:8080/api/v1/dev/otps?email=<email>
```

### Option 3 — Default profile (PostgreSQL + Supabase)

```bash
cp .env.example .env
# fill in values — see Environment Variables section below
./mvnw spring-boot:run
# Windows: .\mvnw.cmd spring-boot:run
```

The database must have pgvector available and allow Flyway to create the `vector` extension used by V29, even when AI keys are omitted. Flyway applies migrations through V41 at startup. Configure SMTP and Supabase Storage using [.env.example](.env.example); set `SWAGGER_ENABLED=true` to explore the API locally.

If Java fails with `libjli.so: invalid ELF header` before Maven starts, check `java -version` and `./mvnw -version`. Repair or select a working Java 21 installation and set `JAVA_HOME`/`PATH` to it; that error comes from the Java runtime installation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3.5, Java 21 |
| Database | PostgreSQL with pgvector, Flyway migrations V1–V41; full-suite validation uses PostgreSQL 17 |
| Auth | JWT via JJWT 0.12.x — type-checked access/refresh tokens backed by persisted `auth_sessions`, rotation, and revocation |
| Token blacklist | PostgreSQL-backed `invalidated_tokens` table — survives restarts |
| Rate limiting | Process-local sliding-window (`RateLimiterService`) — authentication, OTP submissions, guest checkout, pickup, visual search |
| Storage | Supabase Storage (S3-compatible via AWS SDK) |
| Delivery | Database outbox for email, embeddings, and announcement batches; leased jobs with bounded retries and SMTP transport |
| Real-time | SSE (`SseEmitter`) — per-user streams with 25-second heartbeat to prevent proxy idle-timeout disconnections |
| Cache | Spring `ConcurrentMapCache` — categories, popular merch |
| API Docs | springdoc-openapi 2.6 — Swagger UI at `/swagger-ui.html` (disabled by default in prod) |
| Tests | Recorded full-suite validation: 253 tests across 27 classes — unit, H2, PostgreSQL, migrations, and concurrency |
| Logging | Logback — human-readable (dev/docker), structured JSON via logstash-logback-encoder (prod) |

---

## Module Structure

Source root: `src/main/java/com/uitmerch/backend/`. Core domains group entities, repositories, DTOs, services, and controllers into subpackages; newer feature modules keep these classes together in their domain package.

| Module | Responsibility | Access |
|---|---|---|
| `auth`, `user` | Registration, purpose-bound OTPs, persisted sessions, customer profiles | Public auth flows; authenticated logout; CUSTOMER profile |
| `organization`, `merch`, `event` | Storefronts, catalog, categories, events, publication | Public browsing; ORGANIZER writes to owned organizations |
| `cart`, `wishlist`, `order` | Checkout, atomic stock updates, status transitions, pickup schedules | CUSTOMER, owning ORGANIZER; guest order endpoints |
| `notification` | Persisted notifications, SSE, durable announcement fan-out | Own CUSTOMER/ORGANIZER notifications |
| `restock`, `following` | Product subscriptions and organization alert preferences | CUSTOMER |
| `pickup`, `order/history` | One-time pickup credentials and order audit history | Owning CUSTOMER/ORGANIZER; guest receipt exchange |
| `analytics` | Organization-scoped reports | Owning ORGANIZER |
| `campaign` | Preorder campaigns, reservations, deadline finalization | Public discovery; CUSTOMER reservations; owning ORGANIZER management |
| `ai` | Gemini-assisted visual search and merchandise embeddings | Public, rate-limited search |
| `admin` | Account roles/activation, organization approval, order oversight | ADMIN |
| `common` | Security, validation, response models, storage/mail adapters, durable job delivery | Shared infrastructure |

`SecurityConfig` defines public routes and requires authentication for the remaining routes. Controller `@PreAuthorize` checks enforce roles; services enforce ownership.

---

## API Reference

### Auth — `/api/v1/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | None | Register a customer account — sends OTP |
| POST | `/register/organizer` | None | Register an organizer account — sends OTP |
| POST | `/verify-email` | None | Verify email with OTP (5 attempts max; 15-min lockout) |
| POST | `/resend-otp` | None | Re-issue OTP for unverified accounts (silent no-op if email unknown) |
| POST | `/login` | None | Login — returns `token` (access) + `refreshToken` |
| POST | `/refresh` | None | Exchange refresh token for new access + refresh token pair (rotated) |
| POST | `/logout` | Bearer | Revoke the current session and blacklist the access token, invalidating its refresh token too |
| POST | `/forgot-password` | None | Send password-reset OTP (silent no-op if email unknown) |
| POST | `/reset-password` | None | Reset password using OTP |

**Rate limits (per IP):** login 10/15 min · register & resend-otp & forgot-password 5/hr · guest checkout 20/hr

Verification and reset submissions share limits of 10 per account and 30 per IP per 15 minutes, in addition to OTP attempt lockout. Verification OTPs cannot be used for password reset, or vice versa. Refresh rotates the token pair; clients must replace their stored refresh token after every successful refresh.

### User — `/api/v1/customer` *(CUSTOMER)*

| Method | Path | Description |
|---|---|---|
| GET | `/profile` | Get own profile |
| PATCH | `/profile` | Update own profile — fullName, phone, address, avatarUrl (partial) |

### Organization — `/api/v1/organizations` *(ORGANIZER)*

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create an organization (starts as PENDING, awaiting admin approval) |
| GET | `/mine` | List own organizations (paginated) |
| PATCH | `/{orgId}` | Update name, description, logoUrl, coverUrl |

### Public Organizations — `/api/v1/public/organizations`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List ACTIVE organizations (paginated) |
| GET | `/{id}` | Get organization by ID |
| GET | `/{id}/merch` | List published merch for an organization |
| GET | `/{id}/events` | List published/ended events for an organization |

### Merch — `/api/v1/organizations/{orgId}/merchs` *(ORGANIZER)*

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create merch item (org must be ACTIVE; max 10 images per item) |
| GET | `/` | List own merch items — all statuses, paginated |
| GET | `/{id}` | Get own merch item by ID |
| PATCH | `/{id}` | Partial update — name, description, price, stock, imageUrls, status, categorySlug |
| DELETE | `/{id}` | Soft-delete — sets status to ARCHIVED |

### Public Merch — `/api/v1/public/merch`

| Method | Path | Query params | Description |
|---|---|---|---|
| GET | `/` | `?keyword=`, `?category=<slug>`, pagination | List published merch |
| GET | `/popular` | — | Top 10 items by popularity score (cached; evicted on write) |
| GET | `/{id}` | — | Get published merch item by ID |

`POST /api/v1/public/merch/visual-search` accepts multipart field `image` (JPEG, PNG, or WebP up to 5 MB). Configure Gemini keys to use AI search. Limits are 5 requests/minute per IP and 20/minute per process, with at most two concurrent searches.

### Categories — `/api/v1/categories`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all 7 categories ordered by display order (cached) |

Categories seeded:

| Slug | Display name |
|---|---|
| `trang-phuc` | Trang phục |
| `tui-balo` | Túi & Balo |
| `do-dung` | Đồ dùng |
| `phu-kien-ca-nhan` | Phụ kiện cá nhân |
| `luu-niem` | Đồ lưu niệm |
| `qua-tang-handmade` | Quà tặng & Handmade |
| `combo-hop-qua` | Combo & Hộp quà |

### Cart — `/api/v1/customer/cart` *(CUSTOMER)*

| Method | Path | Description |
|---|---|---|
| GET | `/` | Get active cart with full merch details and subtotals |
| POST | `/items` | Add merch item to cart (409 if already exists) |
| PATCH | `/items/{itemId}` | Update cart item quantity |
| DELETE | `/items/{itemId}` | Remove item from cart |
| POST | `/checkout` | Checkout — creates orders grouped by organization; stock deducted atomically |

### Orders — Customer `/api/v1/customer/orders` *(CUSTOMER)*

| Method | Path | Description |
|---|---|---|
| GET | `/` | List own orders (`?status=` filter, paginated) |
| GET | `/{id}` | Get order by ID |
| POST | `/instant` | Instant order — single item without going through cart |
| PATCH | `/{id}/cancel` | Cancel a PENDING order (body: `cancelReason`, optional `cancelReasonNote`) — stock restored |

### Orders and Pickup Schedules — `/api/v1/organizations/{orgId}` *(ORGANIZER)*

| Method | Path | Description |
|---|---|---|
| GET | `/orders` | List org orders (`?status=` filter, paginated) |
| GET | `/orders/{id}` | Get order by ID |
| PATCH | `/orders/{id}/status` | Advance PENDING → CONFIRMED or CONFIRMED → READY for a single order |
| PATCH | `/orders/{id}/checkin` | Mark READY order as COMPLETED (campus pickup check-in) |
| PATCH | `/orders/{id}/cancel` | Cancel PENDING or CONFIRMED order (body: `cancelReason`, optional `cancelReasonNote`) — stock restored |
| POST | `/pickup-schedules` | Create a pickup schedule for a batch of CONFIRMED orders — transitions them all to READY and sends customer notifications |
| GET | `/pickup-schedules` | List org pickup schedules (paginated) |
| GET | `/pickup-schedules/{scheduleId}/orders` | List orders assigned to a pickup schedule (check-in list) |

**Order status transitions:**

```
PENDING → CONFIRMED → READY → COMPLETED
PENDING → CANCELLED                          (stock restored)
CONFIRMED → CANCELLED                        (stock restored)
```

`READY` is reached via `createPickupSchedule` (batch) or `updateOrderStatus` (single). `COMPLETED` is set by `checkInOrder` when the customer physically collects at campus.

Notifications sent at each stage:
- **Order placed** — confirmation email + `ORDER_PLACED` in-app notification to customer; `NEW_ORDER` in-app notification to organizer
- **CONFIRMED / READY / COMPLETED** — status-update email + in-app notification to customer
- **CANCELLED** — dedicated cancellation email to customer (and org owner if customer-cancelled); `ORDER_CANCELLED` in-app notification to organizer

### Orders — Public `/api/v1/public/orders`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Guest checkout — no account required; rate-limited at 20/hr per IP |
| GET | `/{orderId}?email={guestEmail}` | Track a guest order — email must match the one used at checkout |

### Wishlist — `/api/v1/customer/wishlist` *(CUSTOMER)*

| Method | Path | Description |
|---|---|---|
| GET | `/` | Get wishlist with full merch details |
| POST | `/{merchId}` | Add merch item to wishlist (409 if already exists) |
| DELETE | `/{merchId}` | Remove merch item from wishlist |

### Notifications — Customer `/api/v1/customer/notifications` *(CUSTOMER)*

| Method | Path | Description |
|---|---|---|
| GET | `/stream` | SSE stream — real-time push (ORDER_PLACED, ORDER_READY, etc.) |
| GET | `/` | List own notifications (paginated, newest first) |
| GET | `/unread-count` | Get count of unread notifications |
| PATCH | `/{id}/read` | Mark a single notification as read |
| PATCH | `/read-all` | Mark all notifications as read |

### Notifications — Organizer `/api/v1/organizer/notifications` *(ORGANIZER)*

| Method | Path | Description |
|---|---|---|
| GET | `/stream` | SSE stream — real-time push (NEW_ORDER, ORDER_CANCELLED) |
| GET | `/` | List own notifications (paginated, newest first) |
| GET | `/unread-count` | Get count of unread notifications |
| PATCH | `/{id}/read` | Mark a single notification as read |
| PATCH | `/read-all` | Mark all notifications as read |

### Events — `/api/v1/organizations/{orgId}/events` *(ORGANIZER)*

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create event (starts as DRAFT) |
| GET | `/` | List own events (paginated) |
| GET | `/{id}` | Get own event by ID (includes attached merch) |
| PATCH | `/{id}` | Update title, description, coverUrl, dates, or status |
| DELETE | `/{id}` | Delete an owned event (HTTP 204) |
| POST | `/{id}/merch` | Attach a merch item with body `{"merchId":"UUID"}` (must belong to same org) |
| DELETE | `/{id}/merch/{merchId}` | Detach a merch item |

**Event status transitions:** DRAFT → PUBLISHED or CANCELLED; PUBLISHED → DRAFT, ENDED, or CANCELLED. ENDED and CANCELLED are terminal.

### Public Events — `/api/v1/public/events`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List PUBLISHED and ENDED events (paginated) |
| GET | `/{id}` | Get event by ID (PUBLISHED or ENDED only) |

### Admin — `/api/v1/admin` *(ADMIN)*

| Method | Path | Description |
|---|---|---|
| GET | `/users` | List all users (`?role=` filter, paginated) |
| PATCH | `/users/{id}/role` | Change a user's role |
| PATCH | `/users/{id}/active?active=false` | Ban / deactivate a user (login is blocked immediately) |
| GET | `/organizations` | List all organizations (`?status=` filter, paginated) |
| PATCH | `/organizations/{id}/status` | Approve / reject / deactivate an organization — sets to ACTIVE/PENDING/INACTIVE; deactivating also archives all its PUBLISHED merch |
| GET | `/orders` | List all orders (`?status=` filter, paginated) |

---

## New Feature APIs

These features are implemented in the backend. Frontend screens and QR rendering require client integration. Paths in the tables below are relative to `/api/v1`; IDs are UUIDs. Replace the illustrative IDs and dates in request examples with current values.

### Restock subscriptions

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/customer/restock-subscriptions` | CUSTOMER | Subscribe, or update an existing subscription's email preference |
| GET | `/customer/restock-subscriptions` | CUSTOMER | List own enabled subscriptions, paginated |
| DELETE | `/customer/restock-subscriptions/{merchId}` | CUSTOMER | Disable own subscription |

```json
{"merchId":"00000000-0000-0000-0000-000000000001","emailEnabled":false}
```

`emailEnabled` defaults to true when omitted from an explicit subscription request. Pass false for in-app alerts only. Alerts are triggered by stock changing from zero to positive, including stock restored by cancellation, for PUBLISHED products in ACTIVE organizations. Each stock cycle is deduplicated per subscriber; ordinary positive-to-positive stock updates do not alert.

### QR pickup and order history

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/customer/orders/{orderId}/pickup-token` | CUSTOMER | Issue a credential for an owned READY order; no body |
| POST | `/organizations/{orgId}/orders/pickup/verify` | Owning ORGANIZER | Validate a credential without consuming it |
| POST | `/organizations/{orgId}/orders/pickup/checkin` | Owning ORGANIZER | Consume the credential and complete the order atomically |
| POST | `/public/orders/{orderId}/pickup-receipt` | Public | Request an emailed credential for a guest order; generic HTTP 202 |
| POST | `/public/orders/{orderId}/pickup-token` | Public | Exchange the guest receipt credential once for a pickup token |
| GET | `/customer/orders/{orderId}/history` | Owning CUSTOMER | Paginated order audit history |
| GET | `/organizations/{orgId}/orders/{orderId}/history` | Owning ORGANIZER | Paginated order audit history |

Token issuance returns `data.orderId`, `data.token`, `data.pickupScheduleId`, and `data.expiresAt`. The client renders the opaque 43-character token as a QR. It expires after 30 minutes; reissuing it invalidates the previous token. Pickup/receipt tables store credential hashes. Manual completion and cancellation also revoke pickup tokens.

Organizer verify/check-in body:

```json
{"token":"REPLACE_WITH_43_CHARACTER_PICKUP_TOKEN","pickupScheduleId":null}
```

For a scheduled order, send its assigned schedule UUID. The schedule must match and its pickup date cannot be in the future in `Asia/Ho_Chi_Minh`. Verify leaves the token usable; only check-in consumes it. Parallel check-ins have one successful completion.

Guest flow:

1. Request `pickup-receipt` with `{"email":"guest@example.com"}`. Eligible matching guest orders receive a 15-minute receipt credential by email; the HTTP response does not expose it or disclose whether the order matches.
2. Exchange it at the public `pickup-token` endpoint using `{"receiptToken":"REPLACE_WITH_EMAILED_43_CHARACTER_CREDENTIAL"}`. A newer receipt request replaces the older credential.
3. Render the returned pickup token and use the organizer verify/check-in endpoints.

History includes previous/new status, actor, source, pickup schedule, and timestamp. Token issuance records a READY → READY history event without storing the raw pickup token in history.

Limits: customer token issuance 20/minute; organizer verify/check-in share 120/minute; guest receipt requests 5/minute per IP and 3/15 minutes per order; guest token exchange 20/minute per IP.

### Organization following

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/customer/following/{orgId}` | CUSTOMER | Follow an ACTIVE organization; body is optional |
| PATCH | `/customer/following/{orgId}` | CUSTOMER | Update selected preferences on an existing follow |
| DELETE | `/customer/following/{orgId}` | CUSTOMER | Unfollow |
| GET | `/customer/following` | CUSTOMER | List own follows and preferences, paginated |

```json
{"notifyMerch":true,"notifyEvents":true,"emailEnabled":false}
```

Defaults enable merchandise/event alerts and disable email. Omitted fields preserve existing preferences. Only the first publication of each product/event triggers `MERCH_PUBLISHED`/`EVENT_PUBLISHED`; edits and republication do not repeat the alert. Historical publications are backfilled during migration without sending old announcements.

### Organizer analytics

`GET /api/v1/organizations/{orgId}/analytics?from=2026-09-01&to=2026-09-30` requires ORGANIZER and ownership of the organization.

Dates are inclusive, with a maximum window of 366 days. The default is the last 30 days through today in `Asia/Ho_Chi_Minh`.

| Response field | Meaning |
|---|---|
| `orders` | Current-status counts, completed quantity/value, non-cancelled value, PAID value excluding cancelled orders, and cancellation rate from 0 to 1 |
| `inventory` | Current product counts and published stock, independent of the order date window |
| `topProducts` | Up to 20 products ranked by completed quantity, using order-snapshot names and values |
| `dailyOrders` | Orders grouped by creation date; days without orders are omitted |
| `pickupWorkload` | READY/COMPLETED counts by pickup schedule date, including orders created outside the order window |

Order reports use creation dates and current status. `completedOrderValue` and `paidOrderValue` measure different things: completion does not automatically mark an order PAID.

### Preorder campaigns

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/organizations/{orgId}/campaigns` | Owning ORGANIZER | Create a campaign in an ACTIVE organization |
| GET | `/organizations/{orgId}/campaigns` | Owning ORGANIZER | List own campaigns, paginated |
| POST | `/organizations/{orgId}/campaigns/{id}/cancel` | Owning ORGANIZER | Cancel an ACTIVE campaign and release its reservations |
| GET | `/public/campaigns` | Public | Paginated summaries for ACTIVE organizations |
| GET | `/public/campaigns/{id}` | Public | Detail with variants for an ACTIVE organization |
| POST | `/customer/campaigns/{id}/reservations` | CUSTOMER | Reserve a variant with an idempotent request ID |
| GET | `/customer/campaign-reservations` | CUSTOMER | List own reservations, paginated |

Create body:

```json
{
  "title":"UIT shirt preorder",
  "description":"Blue shirts, size M",
  "minimumQuantity":10,
  "deadline":"2026-10-20T16:59:59Z",
  "variants":[
    {"merchId":"00000000-0000-0000-0000-000000000001","label":"Blue / M"}
  ]
}
```

Use 1–20 distinct existing PUBLISHED SKUs belonging to the organization. Variant prices are captured at creation. The minimum is 1–100,000 units and cannot exceed available stock; the deadline must be in the future within 90 days. A SKU can participate in only one ACTIVE campaign at a time.

Reservation body:

```json
{
  "merchId":"00000000-0000-0000-0000-000000000001",
  "quantity":2,
  "requestId":"00000000-0000-0000-0000-000000000002",
  "note":"Optional pickup note"
}
```

Quantity must be 1–100. Generate a new `requestId` UUID per operation and reuse it for retries of that same payload. Replays return the original reservation/order, even after closure; changing the payload with the same ID returns a conflict. The response contains `data.reservation` and `data.order`. Each campaign allows up to 1,000 reservation records, including cancelled ones.

Reservations create PENDING customer orders and deduct stock atomically. ACTIVE campaign SKUs are purchased through this endpoint; ordinary cart, instant, and guest checkout reject them. Customers can cancel their pending order through the existing customer cancellation API. Organizer confirmation, scheduling, and completion wait for campaign success.

At the deadline, the scheduler counts non-cancelled reservations. An eligible campaign meeting its minimum becomes SUCCEEDED, keeping orders reserved for fulfillment. Otherwise it becomes FAILED. Failure or organizer cancellation cancels pending reservation orders, records history, and restores stock once. Repeating closure returns the terminal state. No payment gateway or automatic refund is introduced.

### Delivery behavior

Restock and publication announcements are persisted with their triggering transaction and delivered in batches of 50. Fan-out checks current account eligibility and subscription/follow preferences. Preferences changed before fan-out can suppress delivery; changes cannot recall email already queued by a completed batch.

In-app alerts are deduplicated. Email uses at-least-once outbox delivery: a crash after SMTP succeeds and before the job is marked DONE can cause a duplicate email. Notifications add `relatedMerchId`, `relatedOrgId`, and `relatedEventId` alongside the existing `relatedOrderId`.

---

## API Authentication

1. `POST /api/v1/auth/login` → copy `data.token`
2. Swagger UI → click **Authorize** → paste token → **Authorize**
3. Use endpoints permitted by the account's role and ownership

Swagger is enabled automatically in `dev` and otherwise disabled by default. Enable it with `SWAGGER_ENABLED=true` for the default/Docker-container setup. Protected requests use `Authorization: Bearer <access-token>`; refresh requests send `{"refreshToken":"..."}` to `/api/v1/auth/refresh`.

JWT validation checks the persisted session and current account state/role. Logout revokes the current session. Password reset and account security changes invalidate existing credentials through the account's authentication version.

JSON controller responses use this envelope, omitting null fields:

```json
{
  "success": true,
  "message": "Human-readable string",
  "data": { },
  "traceId": "uuid"
}
```

Existing list endpoints typically return an array in `data` and pagination in `meta`:

```json
{
  "page":0,
  "pageSize":20,
  "totalElements":100,
  "totalPages":5,
  "hasNext":true,
  "hasPrevious":false
}
```

The new restock, following, history, campaign, and reservation lists return a Spring `Page` in `data` instead, with entries in `data.content` and pagination fields inside `data`. Use `?page=0&size=20`; the configured default size is 20 and maximum is 200.

SSE endpoints return `text/event-stream`. They accept a bearer header or `?token=<access-token>` on the two notification stream routes for browser EventSource clients. Streams are bound to session validity/access expiry; reconnect with current credentials. Authentication-filter failures may use a smaller JSON error body than controller errors.

Controller errors have `success: false`, a descriptive `message`, and optional validation field errors in `data`. Malformed bodies/parameters return 400; denied roles/ownership return 403. Missing or invalid credentials return 401. OTP submission, pickup, and visual-search throttling return 429 with `Retry-After`; the older login/registration/guest-checkout throttles return validation errors.

---

## Sample Data

### Dev / Docker profile (DevDataInitializer)

Auto-seeded when the Spring `dev` or explicitly selected `docker` profile is active. Skipped if data already exists. The provided Compose configuration uses the default profile, so it does not use these accounts.

| Email | Password | Role | State |
|---|---|---|---|
| `admin@uit.edu.vn` | `Admin123` | ADMIN | — |
| `org1@uit.edu.vn` | `Org12345` | ORGANIZER | ACTIVE org · has merch, events, orders |
| `org2@uit.edu.vn` | `Org12345` | ORGANIZER | PENDING org · test admin approval |
| `cust1@uit.edu.vn` | `Cust1234` | CUSTOMER | Active cart · wishlist · 2 orders |
| `cust2@uit.edu.vn` | `Cust1234` | CUSTOMER | 1 order READY (campus pickup) |

### PostgreSQL demonstration seed (V12–V16 migrations)

Academic demonstration data referencing 14 UIT clubs and organizations, with 35 merch items across all 7 categories.
All seeded accounts use password `UIT@2025`.

| Email | Role | Notes |
|---|---|---|
| `admin@uitmerch.edu.vn` | ADMIN | Platform administrator |
| `cs.khmt@uit.edu.vn` | ORGANIZER | Khoa Khoa học Máy tính — ACTIVE org |
| `uitstore@uit.edu.vn` | ORGANIZER | UIT Store — ACTIVE org |
| `handmade.xtn@uit.edu.vn` | ORGANIZER | Đội hình Handmade — ACTIVE org |
| *(11 more organizers)* | ORGANIZER | See V12 migration for full list |
| `nguyen.van.an@student.uit.edu.vn` | CUSTOMER | Active cart + wishlist + 2 orders |
| `tran.thi.bich@student.uit.edu.vn` | CUSTOMER | Active cart + wishlist + 1 order |
| `le.minh.cuong@student.uit.edu.vn` | CUSTOMER | Active cart + wishlist + 1 order |
| `pham.hong.duc@gmail.com` | CUSTOMER | 1 order READY (campus pickup) |
| `hoang.thu.em@gmail.com` | CUSTOMER | 1 order COMPLETED |

5 events seeded (PUBLISHED, DRAFT, ENDED) with event–merch links.
8 orders seeded covering all status values including 2 guest orders.

---

## Environment Variables

Start from [.env.example](.env.example). The default Spring profile and supplied Compose service require database, JWT, mail, and storage configuration. The H2 `dev` profile provides development values. Source configuration is in [application.yaml](src/main/resources/application.yaml) and [application-dev.yaml](src/main/resources/application-dev.yaml).

### Core

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `PORT` | *(unset)* | Overrides `SERVER_PORT` when supplied by a hosting environment |
| `APP_JWT_SECRET` | *(required)* | JWT signing secret — at least 32 UTF-8 bytes; validated at startup |
| `APP_JWT_EXPIRATION` | `86400000` | Access token TTL (ms) — 24 h |
| `APP_JWT_REFRESH_EXPIRATION` | `604800000` | Refresh token TTL (ms) — 7 days |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma-separated allowed CORS origins |
| `APP_TRUSTED_PROXY_IPS` | *(empty)* | Comma-separated IPs whose `X-Forwarded-For` is trusted for rate limiting — set to your load-balancer IP |
| `SWAGGER_ENABLED` | `false` | Set to `true` to enable Swagger UI and OpenAPI schema in production |

### Database

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/uitmerch` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |

### Mail

| Variable | Default | Description |
|---|---|---|
| `MAIL_USERNAME` | — | Gmail address used as SMTP sender |
| `MAIL_PASSWORD` | — | Gmail App Password (not your account password) |
| `APP_MAIL_FROM_NAME` | `UITMerch` | Display name shown in email `From:` header |
| `MAIL_SMTP_AUTH` | `true` | — |

The `dev` and explicitly selected `docker` profiles use `DevEmailService` instead of SMTP. OTPs are readable at `GET /api/v1/dev/otps?email=<email>`. In the default profile, application services queue emails in `background_jobs`; the scheduler sends them after commit and retries failed attempts. SMTP is not called on the request thread.

### Supabase Storage

| Variable | Description |
|---|---|
| `SUPABASE_STORAGE_ENDPOINT` | S3-compatible endpoint, e.g. `https://<project>.supabase.co/storage/v1/s3` |
| `SUPABASE_STORAGE_REGION` | AWS region, e.g. `ap-southeast-2` |
| `SUPABASE_STORAGE_S3_ACCESS_KEY_ID` | S3 access key from Supabase dashboard |
| `SUPABASE_STORAGE_S3_SECRET_KEY` | S3 secret key |
| `SUPABASE_PROJECT_URL` | Public base URL, e.g. `https://<project>.supabase.co` |

> Storage is inactive in `dev` and `docker` profiles — `DevStorageService` returns placeholder URLs.

### AI search

| Variable | Default | Description |
|---|---|---|
| `GEMINI_API_KEYS` | *(empty)* | Comma-separated Gemini API keys for rotation |
| `GEMINI_API_KEY` | *(empty)* | Single-key fallback when the key list is not configured |

Keys are optional for starting the application, but required to use Gemini-assisted visual search. PostgreSQL still needs the `vector` extension for Flyway V29.

### Background processing

These are Spring properties; supply them through application configuration or application arguments.

| Property | Default | Description |
|---|---|---|
| `app.delivery.enabled` | `true` | Run the outbox dispatcher and its cleanup schedule |
| `app.delivery.poll-interval-ms` | `1000` | Delay between outbox dispatch attempts |
| `app.campaigns.enabled` | `true` | Run automatic campaign deadline finalization |
| `app.campaigns.poll-ms` | `60000` | Delay between campaign polls; at most 50 due campaigns per poll |

For example, adjust campaign polling locally:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--app.campaigns.poll-ms=10000"
```

Jobs have a five-minute claim lease and at most five attempts, then become DEAD. Completed/dead job payloads are cleared; finished jobs older than 30 days are removed by daily cleanup. Campaign finalization failures leave the campaign ACTIVE for retry on a later poll. Disabling delivery pauses queued mail/announcements; disabling campaign polling pauses automatic deadline finalization.

Generate a secure JWT secret:
```bash
openssl rand -base64 48
```

---

## Database Migrations

Flyway manages schema versions in `src/main/resources/db/migration/`.
Flyway is **disabled** in the `dev` profile (Hibernate generates schema from entities via `create-drop`).
Never modify existing migration files — add a new `VN+1__description.sql` instead.

Current schema version: **V41**. PostgreSQL must support pgvector and extension creation. Upgrade validation covers both empty → V41 and V34 → V41, preserving stock and suppressing announcements for historical publications.

| Migration | Contents |
|---|---|
| V1 | PostgreSQL ENUMs (`user_role`, `order_status`, `event_status`, etc.) |
| V2 | `users`, `otp_tokens` |
| V3 | `organizations` |
| V4 | `merch_items` |
| V5 | `events`, `event_merch` |
| V6 | `carts`, `cart_items` |
| V7 | `orders`, `order_items` |
| V8 | `wishlists`, `wishlist_items` |
| V9 | Indexes on all FK + frequently queried columns |
| V10 | `users.full_name` NOT NULL |
| V11 | `merch_items.stock >= 0` check constraint |
| V12 | Seed 14 demonstration UIT organizations + 35 merch items |
| V13 | `categories` table + 7 seeded categories + `merch_items.category_id` FK |
| V14 | Seed admin, customers, events, orders, carts, wishlists |
| V15 | `otp_tokens.attempt_count` + `locked_until` for brute-force protection |
| V16 | Seed 5 demonstration events |
| V17 | `merch_images` table (multiple images per item, ordered by position) |
| V18 | Drop `merch_items.image_url` (replaced by `merch_images`) |
| V19 | Allow multiple organizations per owner |
| V20–V22 | Seed real organization logo URLs |
| V23 | `invalidated_tokens` table (persistent JWT blacklist — SHA-256 hash, expires_at) |
| V24 | `users.is_active` column (default `true`) for account ban/deactivation |
| V25 | Rename `order_status` enum values: `READY_FOR_PICKUP → READY`, `SUCCESS → COMPLETED` |
| V26 | Add cancel fields to `orders`: `cancelled_by`, `cancel_reason`, `cancel_reason_note`, `cancelled_at` |
| V27 | `pickup_schedules` table + `orders.pickup_schedule_id` FK |
| V28 | `notifications` table + `notification_type` PostgreSQL enum (in-app notifications) |
| V29 | `merch_embeddings` table + `vector` extension (AI visual search) |
| V30 | Add `ORDER_PLACED` to `notification_type` enum |
| V31 | Add `NEW_ORDER` to `notification_type` enum (organizer notifications) |
| V32 | Add CANCELLED event status |
| V33 | `auth_sessions`, account authentication version, and OTP purpose separation |
| V34 | Durable `background_jobs` outbox for email and embeddings |
| V35 | Add `MERCH_RESTOCKED` notification type |
| V36 | Restock subscriptions, announcement events/deliveries, stock cycles, notification links, ANNOUNCEMENT jobs |
| V37 | Pickup and guest-receipt credential hashes; order history |
| V38 | Add `MERCH_PUBLISHED` and `EVENT_PUBLISHED` notification types |
| V39 | Organization follows/preferences and historical publication backfill |
| V40 | Organization/date indexes for order and pickup analytics |
| V41 | Preorder campaigns, variants, reservations, and request-ID uniqueness |

---

## Running Tests

Unit and H2 tests:

```bash
./mvnw test
# Windows: .\mvnw.cmd test
```

Without `UITMERCH_TEST_DATABASE_URL`, PostgreSQL-dependent suites are skipped. A successful ordinary Maven run therefore does not verify the complete migration/concurrency suite.

Full suite using a disposable PostgreSQL database (Bash, Docker, and Java 21 required):

```bash
./scripts/test-postgres.sh -q
```

The harness copies backend sources/build configuration into `/tmp/uitmerch-backend-test.*`, creates a fresh database container on a random localhost port, and supplies `UITMERCH_TEST_DATABASE_URL`. It uses `pgvector/pgvector:pg17` by default; override `UITMERCH_TEST_POSTGRES_IMAGE` with a compatible image if needed. The temporary build excludes the project's `.env`. The container is removed on exit, while test reports remain under the printed temporary path in `target/surefire-reports/`.

The [recorded validation on 2 October 2026](../docs/reviews/2026-10-02-backend-feature-validation.json) ran **253 tests across 27 classes, with 0 failures, 0 errors, and 0 skips**, using PostgreSQL 17. The feature-specific suites include:

| Suite | Tests | Notes |
|---|---|---|
| `RestockFeatureTest` | 7 | Stock transitions, subscription preferences, alert eligibility and deduplication |
| `PickupFeatureTest` | 7 | Customer/guest credentials, reissue races, ownership, and check-in |
| `FollowingFeatureTest` | 5 | Preferences, first-publication delivery, suppression, and concurrent follows |
| `AnalyticsFeatureTest` | 4 | Ownership, date bounds, order aggregation, and pickup workload |
| `CampaignFeatureTest` | 11 | Reservations, retries, deadlines, cancellation, and fulfillment restrictions |
| `LockingFeatureTest` | 2 | Concurrent campaign reservation/finalization |
| `MigrationUpgradeFeatureTest` | 1 | V34 → V41 migration with stock preserved and historical alerts suppressed |
| `ApiBoundaryFeatureTest` | 2 | Invalid input and forbidden API responses |

Remaining suites cover authentication, JWT/session revocation, catalog, checkout, stock contention, email/outbox recovery, AI resource limits, and Spring context startup. See the validation artifact for the full per-class results.

Build the application:

```bash
./mvnw clean package
```

---

## Deployment Considerations

The rate limiter, caches, and SSE connections are process-local. Multiple backend instances do not share rate-limit counters or directly deliver SSE events to each other's clients. Persisted notifications remain available through the list APIs. Account/session validity is checked again for connected streams; SSE proxies should disable response buffering and permit the 25-second heartbeat.

---

## Project Conventions

See [`CLAUDE.md`](./CLAUDE.md) for the full coding conventions used throughout this project (response shape, exception handling, HTTP status codes, Swagger requirements, pagination, etc.).

## Frontend campaign context integration

The frontend integration adds four read-only APIs. No migration beyond V41 is required.

| GET endpoint | Access and purpose |
|---|---|
| `/api/v1/public/merch/{merchId}/purchase-context` | Published merch from an ACTIVE organization; indicates the current ACTIVE campaign and whether reservation is required |
| `/api/v1/customer/orders/{orderId}/campaign-context` | Customer owning the order; links the actual reservation/campaign |
| `/api/v1/organizations/{orgId}/orders/{orderId}/campaign-context` | Organizer owning the organization and its order |
| `/api/v1/organizations/{orgId}/campaigns/{campaignId}` | Owner campaign detail, including variants when the organization is INACTIVE |

Order context joins the reservation by order ID, never by a shared SKU. `fulfillmentAllowed` checks the campaign condition only: reserved orders require SUCCEEDED; ordinary orders pass this condition. Existing mutation APIs still enforce ownership, order status and every other restriction. An overdue ACTIVE campaign remains blocked until the server finalizes it.

The [combined validation](../docs/reviews/2026-10-02-frontend-validation.md) ran 257 backend tests across 28 classes, including four new PostgreSQL context/ownership tests, with no failures, errors or skips. The Docker healthcheck respects Render's `PORT`, then `SERVER_PORT`, then 8080.
