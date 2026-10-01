**UITMerch backend bug-fix and feature plan — 1 October 2026**

Scope: Spring Boot backend, database migrations, backend tests, and API documentation. This plan builds on the [code review](2026-10-01-uitmerch.md) and excludes frontend implementation. Backend bug fixes are now implemented in the working tree and have runtime regression coverage. See [the fix review](2026-10-01-backend-fixes.md) for completed work, validation, and rollout details. New product features and additional API/business-policy proposals wait for your approval of the fixes. Validation uses a temporary working Java 21 runtime because the installed system Java has a corrupt `libjli.so`.

Original planning sequence: secure accounts → protect orders/inventory → stabilize notifications/public APIs → ship restock alerts → ship QR pickup. Keep the current modular monolith. Add infrastructure only when a measured requirement justifies it.

**Change-risk assessment**

Used the repository's GitNexus impact-analysis workflow through the local CLI because GitNexus MCP tools are unavailable. Results are upstream analyses with tests excluded by default. Graph risk is distinct from the severity of the underlying bug.

| Symbol | Direct indexed dependents | Affected process groups | GitNexus risk | Planning implication |
| --- | --- | --- | --- | --- |
| `AuthService.verifyEmail` | 1 | 1; email verification controller | LOW | Small call graph, security-sensitive behavior; verify real transaction commits |
| `AuthService.refreshToken` | 2 | 2; login/refresh groups | HIGH | Lower-bound result: two unresolved receiver call sites were omitted. Inspect source before changes and test token issuance, refresh, and revocation together |
| `OrderService.cancelCustomerOrder` | 1 | 1; customer cancellation controller | LOW | Shared inventory effects require cancellation/confirmation/checkout race tests despite the small call graph |
| `MerchService.updateMerch` | 1 | 1; organizer merch update controller | LOW | Coordinate with all stock writers; isolated method tests cannot prove inventory correctness |
| `NotificationService.push` | 3 | 7; checkout, instant orders, cancellation, customer notifications, pickup notifications, check-in, scheduling | CRITICAL | Preserve its contract initially; introduce delivery changes in small steps with regression coverage across all affected flows |

**Warning:** refresh and notification changes have HIGH/CRITICAL impact. Repeat impact analysis on every actual symbol selected for editing; these planning results are not blanket authorization to bypass AGENTS.md checks. Report direct callers, affected flows, and risk before edits. Run GitNexus detect-changes before any commit.

**Phase 0 — Establish a reliable backend test baseline**

- Repair or select a working Java 21 runtime and run the existing Maven tests without changing application behavior.
- Add a disposable PostgreSQL integration-test environment with pgvector available, applying the entire Flyway chain. Testcontainers is an implementation option; keep tests isolated from configured development or hosted databases.
- Cover the Spring-managed service boundaries, authenticated controller requests, SQL constraints, and transaction commits. Mockito remains useful for isolated business rules.
- Capture failing regressions for OTP attempts, deactivated-user refresh, logged-in public checkout, double cancellation, stock editing during checkout, public event merchandise visibility, and named production-profile bean wiring.
- Include multi-organization checkout and bulk-update persistence-context clearing in the regression suite: pending orders, items, cart updates, and notifications must all survive.

Exit criteria: the current suite runs; the PostgreSQL environment can migrate from empty; every selected bug has a regression test or a documented runtime reproduction. Do not treat H2 success as evidence that PostgreSQL enum, locking, and migration behavior works.

**Phase 1 — Authentication and account security**

Relevant modules: `auth`, `common/security`, `common/config`, `admin`.

| Work item | Proposed implementation | Acceptance criteria |
| --- | --- | --- |
| Durable OTP lockout | Separate attempt accounting from the transaction that returns an invalid-code error. Use an atomic increment or locked OTP row; bind codes to verification/reset purpose; ensure resend does not erase an account-level lock | Wrong attempts remain recorded after errors; the configured failure threshold blocks further attempts, including parallel submissions |
| Submission rate limits | Apply limits to verify/reset submissions as well as issuance. Limit by normalized account identifier and client; return HTTP 429 with an appropriate retry indication | Repeated submissions stop before further verification work; responses do not reveal whether an account exists |
| Authoritative account checks | Check active/verified status during refresh; on protected requests validate current account status, session status, and role/version rather than relying solely on stale JWT claims | Disabled accounts cannot refresh or use existing protected sessions; old admin rights disappear after demotion |
| Session revocation | Introduce `auth_sessions` with session ID, user ID, hashed current refresh credential, refresh expiry, and revocation time; add a user authentication version. Issue signed access/refresh tokens with session/version claims and unique token IDs | Password reset and sensitive role/account changes invalidate earlier sessions; logout revokes the session's refresh capability too |
| Atomic refresh rotation | Match and replace the stored refresh hash atomically after token-type/issuer/expiry checks. Generate unique credentials even within the same second | Two simultaneous uses cannot both rotate successfully; a consumed credential cannot be reused |
| Explicit API token validation | Require access-token type and expected issuer for API requests; require refresh type for refresh | Refresh tokens cannot be used as API bearer credentials |
| SSE credential handling | Add a short-lived, stream-scoped ticket issued through an authenticated backend endpoint; redact credentials from logs. Keep transition support for existing consumers while documenting the new contract | A ticket cannot authorize general REST requests; expired/revoked sessions stop receiving private updates |

Migration approach: add session storage and authentication-version fields in new Flyway migrations. Preserve the existing auth response field names. Tokens issued before session/version enforcement will require re-login; document that rollout explicitly. Change access-token lifetimes when consuming clients can handle refresh correctly.

Keep security state authoritative across processes. Start with database-backed session checks; add caching only with a defined invalidation guarantee. Review public demo admin accounts before using the same environment for private data.

**Phase 2 — Orders, inventory, and request correctness**

Relevant modules: `order`, `cart`, `merch`, `wishlist`.

1. **Make order transitions atomic.** Lock the order row before validating cancellation, confirmation, schedule assignment, or completion. Use the same transition service for customer and organizer actions. Only the winning cancellation restores stock. Acquire multiple rows in a deterministic order to prevent avoidable deadlocks.
2. **Make all stock writers cooperate.** Retain guarded atomic deductions, but require merchandise edits/restocking and cancellation restoration to use the same inventory boundary. Lock merchandise before editing its current state. Never save a stale absolute stock snapshot. If optimistic versions are used instead, bulk stock queries must update/check those versions too.
3. **Add checkout idempotency.** Support an `Idempotency-Key` header on cart, instant, and public checkout. Persist request identity, payload hash, status, and the resulting order-ID set. Repeating the same operation returns the original result; changing its payload returns a conflict. An operation ID must cover the whole per-organization order set.
4. **Link authenticated purchases in the backend.** For `POST /api/v1/public/orders`, a valid CUSTOMER principal should create customer-linked orders; an anonymous request creates guest orders. Read identity from validated authentication, never request-body user IDs or email matching. A supplied invalid/expired bearer credential should produce 401 instead of silently creating a guest purchase. Explicitly reject unsupported authenticated roles. Preserve the existing list response shape and document the account-aware behavior. Reuse the same creation logic as authenticated checkout.
5. **Harden request validation.** Cascade validation into guest items, reject null list elements and nonpositive quantities, bound request/list/string sizes, and enforce positive quantities in the inventory boundary. Return clear 400 errors before database failures. Validate guest contact formats and distinct pickup order IDs; reject past pickup dates using the campus timezone.
6. **Correct cart/wishlist read transactions.** Make getters return an empty view without persistence, or use an explicit write transaction for creation/reactivation. Cover concurrent first creation and the one-cart/one-wishlist uniqueness constraints.
7. **Define reservation expiry.** Persist a PENDING-order reservation deadline. A scheduled worker expires due reservations through the same locked transition and restoration logic. Confirming an order prevents expiry; each expired order releases inventory once. Keep the duration configurable and decide how expiry is represented without overloading cancellation reasons.
8. **Record meaningful state history.** Add order transition audit records containing previous/new state, actor/source, timestamp, and request/operation ID. Keep delivery completion and simulated cash-payment acknowledgment distinct; do not automatically mark every completed order PAID.

Exit criteria: inventory remains correct under concurrent edits/purchases/cancellations; one repeated checkout creates one order set; customer-linked purchases appear in customer order queries; invalid quantities return 400; an expired reservation cannot later be confirmed or release stock twice. Wrong-user/wrong-organization actions remain denied.

**Phase 3 — Delivery reliability, public visibility, and AI stability**

Relevant modules: `notification`, `common/service`, `event`, `ai`, `organization`, `merch`.

| Work item | Proposed implementation | Acceptance criteria |
| --- | --- | --- |
| Durable background work | Add a database outbox for email and embedding jobs, written in the business transaction; dispatch with bounded retries, leases, and deduplication. Preserve the existing in-app notification API at first | Rollback sends no email/index job; committed jobs can recover after restart without duplicate persisted notifications |
| Correct notifications | Escape untrusted HTML text, use the actual cancellation actor, send applicable status messages to guest contact addresses, and expose a durable guest receipt/tracking credential | Organizer cancellation is labeled correctly; injected markup is treated as text; guests can retrieve their own order without relying on email as a secret |
| Public data boundaries | Filter public event merchandise to eligible published items; require ACTIVE organizations for event publication; apply organization suspension policy to public queries | Draft/archived products and disallowed organization content never appear through public event endpoints |
| Cache correctness | Define popularity-cache TTL/invalidation on visibility, stock, and order changes; prefer current data projections over cached stock snapshots | Archived products disappear promptly; featured stock and eligibility are current |
| Bound AI resource use | Add endpoint/global quotas, concurrency bounds, connect/request timeouts, bounded retry, and controlled unavailable responses | Stalled providers terminate within a defined budget; excess searches do not trigger more provider calls |
| Index correctness | Create embeddings after commit; retry recoverable failures; filter published/eligible merchandise in SQL before nearest-neighbor LIMIT | New committed products eventually become searchable; archived items cannot crowd valid items out of results |
| Production configuration | Align AI production profile predicates; move springdoc configuration to the YAML root; test default and named production profiles | Production-profile wiring works; Swagger/API docs obey their enable flags |
| Authorized uploads | Expose backend image-upload or scoped upload-signing endpoints backed by existing StorageService; enforce user/organization ownership, size, and decoded file type | An organizer cannot upload into another organization's path; spoofed file headers are rejected; callers never receive privileged storage credentials |

Use a database outbox before adding a dedicated queue service. If deployment later uses multiple instances, add notification fan-out and shared rate-limit/revocation behavior as a separate measured change. Inspect actual Supabase bucket policies before defining the upload contract; those policies were not available in the local review.

Exit criteria: transaction rollback produces no external notification, jobs survive process interruption, public-data boundaries hold, and upstream failure cannot tie up request threads indefinitely.

**Feature release A — Restock subscriptions**

Why first: a small backend extension with a clear use case, reusing merchandise, wishlist, and notifications.

- Add `restock_subscriptions(user_id, merch_id, enabled, last_notified_stock_cycle, created_at)` with uniqueness on user/product. Subscribe explicitly rather than automatically opting every wishlist entry into alerts.
- Proposed API: `POST /api/v1/customer/restock-subscriptions` with merch ID; `GET` for the user's subscriptions; `DELETE /api/v1/customer/restock-subscriptions/{merchId}` to unsubscribe.
- Detect the inventory transition from zero to positive through the centralized inventory boundary, including stock restored after cancellation where relevant. Notify only when merchandise is still public and its organization is eligible.
- Add notification type `MERCH_RESTOCKED` and optional `relatedMerchId` in Notification/NotificationResponse. Keep `relatedOrderId` for current order notifications.
- Create notification/outbox records atomically with a restock-cycle deduplication key. Batch fan-out with bounded processing; do not send email inside the stock-update transaction.

Acceptance: subscribed users get one alert per restock cycle; positive-to-positive changes do not spam; unsubscribe suppresses future alerts; archived merchandise triggers no alert; retries cannot duplicate the same cycle notification.

Dependencies: Phase 2 inventory boundary and Phase 3 outbox/notification regression coverage. Relative size: medium.

**Feature release B — One-time QR pickup verification**

The backend returns an opaque pickup credential for a client to render as a QR code; generating a QR image is not required for the API.

- Add `pickup_tokens(order_id, token_hash, expires_at, consumed_at, revoked_at)` with appropriate uniqueness/indexes. Generate cryptographically random credentials; persist only a hash.
- Proposed API: `POST /api/v1/customer/orders/{orderId}/pickup-token`; `POST /api/v1/organizations/{orgId}/orders/pickup/verify`; `POST /api/v1/organizations/{orgId}/orders/pickup/checkin`. Verification is a scoped read; check-in consumes the token and completes the order in one transaction.
- Token issuance requires customer ownership and a READY order. A guest may obtain a token through a verified, order-scoped guest receipt credential. Tokens must not encode customer names, phone numbers, or addresses.
- Organizer verification/check-in requires authenticated ownership of the organization, matching order/slot, a valid unexpired token, and READY state. Possession of the token alone is insufficient.
- Retain the existing manual check-in endpoint, sharing the same atomic transition logic. Reissuing a pickup token revokes the previous token.
- Record actor, schedule, time, and scan/manual source in the order audit trail. Keep cash acknowledgment explicit if modeled.

Acceptance: wrong organizers are denied; expired/reissued/cancelled tokens fail; parallel scans complete one order once; manual completion invalidates later scans; a successful pickup creates the expected customer notification and audit record.

Dependencies: Phase 1 identity/session checks, Phase 2 atomic transitions, Phase 3 durable notifications. Relative size: medium.

**Follow-on backend features**

| Feature | Initial backend scope | Dependency / relative size |
| --- | --- | --- |
| Organization following | Follow/unfollow/list endpoints, unique user/org relation, explicit notification preferences, events for new published merchandise | Notification targets/preferences and outbox; medium |
| Organizer analytics | Organization-scoped aggregates for completed quantities, reservations, cancellation rate, inventory, and pickup workload; bounded date ranges. Separate order value from acknowledged payment revenue | Trustworthy order state/audit data; medium |
| Preorder campaigns | Campaign deadline/minimum quantity, variants, reservations, success/failure lifecycle, and once-only reservation release; separate campaign state from order state | Inventory/idempotency/expiry foundations; large. Defer until the earlier releases are stable |

**Suggested work packages**

| Order | Reviewable package | Required checks |
| --- | --- | --- |
| 1 | PostgreSQL integration harness and bug reproductions | Isolated database, full migrations, existing test baseline |
| 2 | OTP attempt persistence, purpose, and limits | Commit-after-error, concurrency, lockout, enumeration behavior |
| 3 | Sessions, token validation, refresh/logout revocation | Disabled/demoted/reset users, replay, same-second uniqueness |
| 4 | Order transitions, inventory boundary, checkout idempotency | Cancel/confirm/edit races, multi-org checkout, rollback |
| 5 | Account-aware checkout, validation, cart/wishlist transactions | Authentication cases, ownership, request constraints, unique creation |
| 6 | Reservation expiry and audit history | Worker retries, once-only release, confirmation race |
| 7 | Public visibility, configuration, bounded AI | Cross-role access, profile wiring, provider timeout/quota |
| 8 | Outbox, guest receipts, mail safety, embedding delivery, upload authorization | Rollback, restart, deduplication, ownership, HTML escaping |
| 9 | Popularity invalidation and remaining pickup/payment logic | Visibility/current stock, date/duplicate validation, explicit payment semantics |
| 10 | Restock subscription APIs | Stock-cycle fan-out, preferences, retry deduplication |
| 11 | QR pickup APIs | Replay, cross-organization access, parallel scan, manual fallback |

Each package should include its necessary migrations and focused tests. Allocate Flyway versions from the current latest migration at implementation time; preserve previously applied migrations. Check enum additions and deployment ordering before enabling new writers. Document request/response changes in OpenAPI and keep existing response fields stable where possible.

Definition of done per package: GitNexus impact reviewed; bug/feature acceptance criteria verified on PostgreSQL; relevant Maven tests pass; full migrations validate; role/ownership checks hold; detect-changes shows expected scope before commit. Actual calendar estimates should be made after Phase 0 establishes the runtime baseline.
