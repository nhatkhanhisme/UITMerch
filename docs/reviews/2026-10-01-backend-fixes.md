**Backend fixes ready for review — 1 October 2026**

This change implements backend correctness and security fixes from the [backend plan](2026-10-01-backend-plan.md). New product features remain gated on your approval of the fixes. Changes are available in the working tree for review.

| Area | Result | Main review entry |
| --- | --- | --- |
| OTP | Failed attempts commit despite an invalid-code error. User-row locks serialize submissions and issuance. Reissuing preserves live failure counts and cannot erase lockout. Verification/reset codes have separate purposes; invalid-account and lockout responses remain generic. Submission limits return 429 and Retry-After. | `AuthService`, `AuthController`, V33 |
| Sessions | Refresh credentials rotate once using a locked session row and stored hash. Tokens have unique IDs, issuer/type checks, session IDs and account versions. Protected requests check current account state. Password reset, role changes and activation changes invalidate old credentials; logout revokes its session. Expired access headers do not block the refresh endpoint. | `AuthSessionService`, `JwtTokenProvider`, `JwtAuthenticationFilter` |
| Revocation/storage | Blacklist checks read authoritative database state across instances. Its duplicate-safe insert also works with H2. Expired sessions are purged. | `TokenBlacklistService`, `InvalidatedTokenRepository` |
| Orders/inventory | Checkout locks merchandise before reading price/status/stock. Metadata edits and archival share those locks. Order state changes lock the order before validation; cancellation restores inventory once. Batch order and inventory locks use stable ordering. Bulk updates flush before clearing managed entities. Nonpositive deductions cannot increase stock. | `OrderService`, `MerchService`, order/merch repositories |
| Public checkout | Valid CUSTOMER authentication links orders to the authenticated user. Anonymous checkout remains available. Supplied invalid credentials return 401; authenticated unsupported roles return 403. Identity never comes from guest email or request-body user IDs. | `PublicOrderController` |
| Validation | Guest item validation cascades into bounded lists with non-null elements. Contact/text sizes are bounded. Pickup rejects duplicate/null order IDs and past dates in Asia/Ho_Chi_Minh. | `GuestOrderRequest`, `PickupScheduleRequest`, `OrderService` |
| Cart/wishlist | Getters that create/reactivate records use write transactions. User-row locks serialize first creation and cart mutations/checkout. Cart mutations reject unavailable merchandise and invalid quantities. | `CartService`, `WishlistService` |
| Visibility/cache | Public event detail filters unpublished merchandise. Event publication requires an active organization. Public catalog/event queries exclude inactive organizations. Popular merchandise is computed from current rows rather than cached indefinitely. | `EventService`, `MerchService`, repositories |
| Mail/indexing | Business transactions enqueue persisted jobs. Workers claim leases, perform provider I/O outside database transactions, retry failures, and recover abandoned leases. Payloads are wiped on completion/permanent failure. Embeddings use current committed product text; startup backfill uses the same queue. | `common/delivery`, `ProdMerchEmbeddingService`, V34 |
| Notifications | Guest receipt/status messages use their contact email. Organizer cancellation is labeled with the actual actor. Email HTML escapes untrusted notes/location/reasons. Existing live streams recheck session/account state before events and heartbeats. | `OrderService`, `JavaMailEmailService`, `SseEmitterManager` |
| AI | Per-client and instance quotas plus two concurrent search slots bound public requests. Connect timeout is 5 seconds; each provider call has a 20-second total budget and at most three key attempts. API keys use headers. JPEG/PNG are decoded after dimension checks; WebP envelopes/chunks and dimensions are checked. Vector SQL filters eligibility before LIMIT. | AI controller/services, `ImageContentValidator` |
| Startup/configuration | Corrected V16's missing SQL semicolon. Enum constants in JPQL are bound as parameters, avoiding Hibernate casts to enum names that do not match Flyway types. Swagger configuration now uses the YAML root and is disabled by default. SMTP timeouts and a scheduling pool are configured. | V16, repositories, `application.yaml` |

**Verification**

The original suite had 174 passing tests. Three added PostgreSQL regressions were also run against the original application code, with only V16's semicolon corrected in the temporary baseline to allow database startup: all three failed as expected (OTP attempt persistence, deactivated refresh, and cascaded guest-item validation).

The current regressions cover simultaneous refresh, cancel/confirm and double-cancel races, overselling, metadata editing during purchase, multi-organization persistence, transaction rollback, customer-linked public checkout, session revocation, live stream cutoff, concurrent first cart/wishlist creation, public visibility, popularity freshness, vector filtering, rate limits, mail escaping, job retry/lease recovery, and embedding jobs. H2 has a separate real login/refresh/logout regression. Named production AI bean wiring and HTTP resource bounds are tested without provider calls.

All 214 tests passed with zero failures, errors or skips in both the fresh-database and V32-to-V34 upgrade runs. The fresh run applied all 34 migrations; the upgrade applied V33 and V34. Detailed results and source hashes are recorded in [validation evidence](2026-10-01-backend-fix-validation.json). GitNexus upstream impact checks and HIGH/CRITICAL warnings were reviewed before source edits; see [impact notes](2026-10-01-backend-fix-impact.md). The graph does not resolve every Spring/JUnit reflective caller. Runtime tests cover those entry points.

Reproduce with Java 21 and Docker:

```sh
backend/scripts/test-postgres.sh
```

The script creates a disposable PostgreSQL database with pgvector and runs the entire Maven suite. It copies the backend into a temporary build directory, applies all Flyway migrations, removes its container on exit, and prints the test report path. PostgreSQL-specific tests skip during an ordinary Maven run unless UITMERCH_TEST_DATABASE_URL is set; the script sets it for its own database.

Validation here used the already-cached `public.ecr.aws/supabase/postgres:17.6.1.167` image through the script's `UITMERCH_TEST_POSTGRES_IMAGE` override. No hosted database was used. The installed system Java cannot start because libjli.so is corrupt; validation used a temporary Temurin Java 21 runtime. Local disk exhaustion also required temporary builds and memory-backed database data.

**Rollout details for approval**

- V33 adds account/session state and OTP purpose; V34 adds durable jobs. Deploy schema and code together. Earlier tokens lack session/version claims and require login again. Earlier OTPs are invalidated because their purpose is ambiguous.
- V16 is the necessary exception to preserving historical migration files: the checked-in SQL cannot migrate a fresh database. Compare any existing deployment's V16 artifact/checksum with the corrected file before release. No existing database history was repaired or edited here.
- SMTP delivery is at least once. A process can fail after a provider accepts mail but before DONE is recorded, so retries can duplicate mail. Persisted in-app notifications remain in the order transaction. Jobs stop after five attempts; DEAD jobs need operational inspection. Expired OTP jobs are discarded; finished jobs are retained for 30 days with cleared payloads.
- Quotas and concurrent search slots are per application instance. Database session checks and stream revocation work across instances, while live notification fan-out still uses each instance's emitter map.
- WebP validation checks container structure and dimensions; the JDK does not provide a full WebP decoder. JPEG/PNG validation includes decoding. Images over 4096 pixels per side are rejected.

**Approval-gated follow-up**

Restock alerts, QR pickup, following, analytics and campaigns are untouched. The plan's optional idempotency-header contract, reservation-expiry policy, audit history, cash acknowledgment, stream tickets, guest receipt credentials and new authorized-upload APIs also remain proposals for the next approved phase. They introduce API or business-policy decisions rather than repairing an existing implementation contract.

Two deployment-dependent security review items remain: actual storage bucket policies and the use of seeded demo accounts in a real deployment. SSE retains the existing query-token compatibility path; replacing full access tokens in stream URLs requires a coordinated client transition to stream-scoped credentials. Guest tracking retains its existing UUID-plus-email contract. These limits should not be read as a claim that every deployment security issue has been eliminated.

Implementation references: [Spring transaction rollback rules](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/transaction/annotation/Transactional.html), [Hibernate enum literal formatting](https://github.com/hibernate/hibernate-orm/blob/6.5.3/hibernate-core/src/main/java/org/hibernate/dialect/PostgreSQLEnumJdbcType.java), [H2 PostgreSQL compatibility](https://h2database.com/html/features.html), and the [WebP container specification](https://developers.google.com/speed/webp/docs/riff_container).
