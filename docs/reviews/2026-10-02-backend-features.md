**UITMerch backend features — implemented 2 October 2026**

All five features were implemented in the requested order after authorization to proceed. Changes are organized into focused commits on `feat/new-features-20261001`, based on bug-fix commit `e81915c`. Frontend implementation and deployment are outside this change. Existing order and notification response fields remain available; notification responses add `relatedMerchId`, `relatedOrgId`, and `relatedEventId`.

**1. Restock subscriptions**

| Method | Path under `/api/v1` | Access |
| --- | --- | --- |
| POST | `/customer/restock-subscriptions` | CUSTOMER |
| GET | `/customer/restock-subscriptions` | CUSTOMER; own subscriptions, paginated |
| DELETE | `/customer/restock-subscriptions/{merchId}` | CUSTOMER; own subscription |

Subscribe body:

```json
{"merchId":"PRODUCT_UUID","emailEnabled":true}
```

Email defaults to true for an explicitly requested restock subscription; pass false for in-app alerts only. Repeating POST updates preferences. Unsubscribe disables the relation; subscribing again starts a new subscription time.

The stock transition from zero to positive creates a durable announcement, including stock returned by order cancellation. Positive-to-positive edits do not alert. Only published merchandise belonging to an ACTIVE organization is eligible. A product-level stock cycle and a unique announcement/user delivery pair suppress duplicate persisted alerts. Fan-out processes 50 recipients per transaction, with durable continuation jobs; inactive/unverified users, disabled subscriptions and subscriptions created after the event are excluded.

**2. QR pickup and order history**

| Method | Path under `/api/v1` | Access |
| --- | --- | --- |
| POST | `/customer/orders/{orderId}/pickup-token` | CUSTOMER; owns READY order |
| POST | `/organizations/{orgId}/orders/pickup/verify` | ORGANIZER; owns organization |
| POST | `/organizations/{orgId}/orders/pickup/checkin` | ORGANIZER; owns organization |
| POST | `/public/orders/{orderId}/pickup-receipt` | Guest receipt request; generic HTTP 202 |
| POST | `/public/orders/{orderId}/pickup-token` | Guest with emailed receipt credential |
| GET | `/customer/orders/{orderId}/history` | CUSTOMER; own order, paginated |
| GET | `/organizations/{orgId}/orders/{orderId}/history` | ORGANIZER; own organization/order, paginated |

Customer token issuance takes no body. The response contains `orderId`, an opaque `token`, `pickupScheduleId`, and `expiresAt`. The client renders the token as a QR; the backend does not return an image. Tokens last 30 minutes. Issuing another token invalidates the previous one.

Organizer verify/check-in body:

```json
{"token":"43_CHARACTER_CREDENTIAL","pickupScheduleId":"SCHEDULE_UUID"}
```

Use null for `pickupScheduleId` when the order has no assigned schedule. Verify reads without consumption. Check-in locks the order, reloads the current token, consumes it and completes the order atomically. An assigned schedule must match the order and cannot have a future pickup date in Asia/Ho_Chi_Minh. Parallel scans have one winner; manual completion or cancellation revokes the token. QR credentials contain no personal data and the pickup/receipt tables persist SHA-256 hashes.

Guest flow: POST `pickup-receipt` with `{"email":"guest@example.com"}`. Matching eligible guest orders receive a 15-minute credential through the email outbox; the HTTP response never returns that credential or reveals whether the order matches. Exchange it once using `{"receiptToken":"EMAILED_43_CHARACTER_CREDENTIAL"}` at the public pickup-token endpoint. The latest receipt request replaces earlier credentials. The queued email payload necessarily contains the short-lived credential; it is distinct from the hash stored in the receipt table.

Rate limits: customer issuance 20/minute; organizer verify/check-in share 120/minute; guest receipt requests 5/minute per client IP and 3 per 15 minutes per order; guest exchange 20/minute per IP. These use the existing rate limiter.

History records previous/current state, source, actor, schedule and timestamp. Sources include CUSTOMER, ORGANIZER, MANUAL, QR, GUEST, SCHEDULE and CAMPAIGN; token issuance records a READY-to-READY event without persisting the raw token. Ownership applies to both history endpoints.

**3. Organization following**

| Method | Path under `/api/v1` | Access |
| --- | --- | --- |
| POST | `/customer/following/{orgId}` | CUSTOMER; active organization |
| PATCH | `/customer/following/{orgId}` | CUSTOMER; own existing follow |
| DELETE | `/customer/following/{orgId}` | CUSTOMER; own follow |
| GET | `/customer/following` | CUSTOMER; own follows, paginated |

POST may omit its body. Default preferences are merchandise alerts on, event alerts on, email off. POST and PATCH accept selected fields:

```json
{"notifyMerch":true,"notifyEvents":true,"emailEnabled":false}
```

Omitted fields preserve current preferences. Unique user/organization relations and a user-row lock make concurrent follows idempotent. First publication of a product/event creates MERCH_PUBLISHED/EVENT_PUBLISHED announcements. Editing or republishing the same item does not spam followers. Existing publications are backfilled as announced during migration, so upgrading does not notify users about historical content.

Following reuses the durable announcement batches and checks preferences at fan-out. Unfollowing, disabling a category, or becoming ineligible before fan-out suppresses that delivery. Preference changes do not recall email jobs already queued by an earlier completed batch.

**4. Organizer analytics**

`GET /api/v1/organizations/{orgId}/analytics?from=2026-09-01&to=2026-09-30` requires ORGANIZER and organization ownership.

Dates are inclusive; queries use an exclusive next-day upper bound. Maximum 366 days. Without dates, the report uses the last 30 days ending on the current Asia/Ho_Chi_Minh date. Order windows use the stored `created_at` date/time representation; this change does not reinterpret historical timestamps.

The typed response contains:

- `orders`: counts by current state, completed item quantity, completed order value, non-cancelled order value, PAID value excluding cancelled orders, and cancellation rate as a fraction from 0 to 1.
- `inventory`: current product counts, published stock units and published products with zero stock, independent of the order date window.
- `topProducts`: up to 20 products ranked by completed quantity, with order-snapshot names and subtotal values.
- `dailyOrders`: days with orders, based on creation date and current order status. Missing days are omitted.
- `pickupWorkload`: READY/COMPLETED counts per schedule in the requested pickup-date range, including orders created outside the order window.

Orders and item quantities are aggregated separately to prevent double-counting order totals. Completion does not imply payment: `completedOrderValue` and `paidOrderValue` are separate. These are current-state aggregates, not historical status snapshots or an accounting ledger.

**5. Preorder campaigns**

| Method | Path under `/api/v1` | Access |
| --- | --- | --- |
| POST | `/organizations/{orgId}/campaigns` | ORGANIZER; owns ACTIVE organization |
| GET | `/organizations/{orgId}/campaigns` | ORGANIZER; own organization, paginated |
| POST | `/organizations/{orgId}/campaigns/{id}/cancel` | ORGANIZER; own campaign |
| GET | `/public/campaigns` | Public; ACTIVE organizations, paginated summaries |
| GET | `/public/campaigns/{id}` | Public; ACTIVE organization, detailed variants |
| POST | `/customer/campaigns/{id}/reservations` | CUSTOMER |
| GET | `/customer/campaign-reservations` | CUSTOMER; own reservations, paginated |

Create body:

```json
{
  "title":"UIT shirt preorder",
  "description":"Blue shirts in two sizes",
  "minimumQuantity":10,
  "deadline":"2026-10-20T16:59:59Z",
  "variants":[
    {"merchId":"BLUE_M_PRODUCT_UUID","label":"Blue / M"},
    {"merchId":"BLUE_L_PRODUCT_UUID","label":"Blue / L"}
  ]
}
```

Each size/color variant uses a distinct existing published SKU from the same organization. Its unit price is captured when the campaign is created. A SKU can belong to one ACTIVE campaign at a time. Deadline must be in the future within 90 days, with 1–20 variants and a positive minimum not exceeding available stock or 100,000 units. Campaigns have a maximum of 1,000 reservation records, including subsequently cancelled records.

Reservation body:

```json
{"merchId":"BLUE_M_PRODUCT_UUID","quantity":2,"requestId":"NEW_UUID_PER_OPERATION","note":"Optional note"}
```

Quantity is 1–100; note is at most 1,000 characters. A reservation creates an ordinary customer-linked PENDING order and deducts stock in the same transaction. The response contains typed `reservation` and `order` objects. Replaying the same user's `requestId` and identical payload returns the original order, including after closure. A changed payload conflicts; concurrent reuse across campaigns cannot create two committed orders or deduct stock twice.

ACTIVE campaign SKUs must be purchased through the reservation endpoint: normal cart, instant and public checkout reject them. Integrating clients should discover these campaigns through the new public campaign APIs. Orders appear in existing customer order APIs, but organizer confirmation, scheduling and completion are blocked until campaign success. Customers can cancel a pending reservation through the existing cancellation endpoint.

Campaign state is separate from order state: ACTIVE → SUCCEEDED, FAILED or CANCELLED. The scheduler runs every 60 seconds by default, selecting up to 50 due campaigns. At deadline, the locked campaign counts reservations whose orders are not cancelled. Success requires the minimum and still-eligible organization/SKUs. Successful orders remain reserved for organizer fulfillment. Failure/cancellation cancels pending orders, records CAMPAIGN history and restores each order's stock once. Closing again returns the persisted terminal state without another release. The closing quantity is stored as a snapshot.

Locks follow campaign → sorted orders → sorted SKU rows. SKU locks prevent concurrent activation of overlapping campaigns; the stock restore ordering also matches PostgreSQL's unsigned UUID ordering. Completion/cancellation never upgrades a held order lock to a campaign lock. Every close is a separate transaction; failed scheduler attempts remain ACTIVE and are retried by a future poll. Set `app.campaigns.enabled=false` to disable that scheduler or `app.campaigns.poll-ms` to change the interval. Tests disable automatic polling and exercise finalization explicitly.

No payment gateway or automatic refund is introduced. PAID is not set automatically. Campaign failure releases stock and updates the existing cash-order workflow.

**Migrations and rollout**

| Version | Change |
| --- | --- |
| V37 | MERCH_RESTOCKED enum value |
| V38 | Restock subscriptions, durable announcement events, notification links, stock cycles, ANNOUNCEMENT jobs |
| V39 | Pickup/guest credential hashes and order history |
| V40 | MERCH_PUBLISHED and EVENT_PUBLISHED enum values |
| V41 | Following preferences and publication backfill flags |
| V42 | Organization/date analytics indexes |
| V43 | Preorder campaigns, variants, reservations and request uniqueness |

Apply all new Flyway migrations before using the new writers. Enum additions are separate versions from subsequent writes. Deployed V33/V34 are preserved; authentication sessions and durable delivery now occupy V35/V36. V16 retains its reviewed semicolon fix and requires the guarded metadata repair described in [environment fixes](2026-10-02-backend-environment-fixes.md) when upgrading the original deployed checksum. Migration tests validate empty → V43 and V34 → V43 with stock preserved and no historical announcement jobs. All runtime database checks used disposable PostgreSQL databases; no configured development/hosted database was migrated.

In-app alert persistence is deduplicated. SMTP transport retains the outbox's at-least-once semantics; a crash after an email is sent but before the job is marked DONE can cause a duplicate email. The existing rate limiter is process-local. These implementation limits remain relevant when deploying multiple instances.

**Commit organization**

| Commit | Work | Snapshot verification |
| --- | --- | --- |
| `7f0dd67` | fix(api): return 400 for malformed input and 403 for denied access | 7 passing tests |
| `2e189d1` | feat(restock): add subscriptions and durable stock-cycle alerts | 55 passing tests |
| `d7b0e4a` | feat(pickup): add one-time QR credentials and order history | 40 passing tests |
| `16026bb` | feat(following): notify customers about organization publications | 39 passing tests |
| `c125c81` | feat(analytics): add organization-scoped sales and pickup reports | 4 passing tests |
| `62a399b` | feat(preorder): add campaign reservations and deadline finalization | 44 passing tests |
| `a669202` | test(backend): cover request errors, pickup reissue races and upgrades | 253 passing tests; exact match to the previously validated full suite |

The documentation commit follows these seven code/test commits. GitNexus staged-change detection ran before each commit. Feature snapshots were exported directly from the Git index and tested on isolated databases, so earlier feature commits do not depend on code introduced by later commits.

**Change scope and impact review**

GitNexus upstream impacts were checked before editing existing symbols. The combined `detect_changes` result includes new files through a temporary Git index, leaving the real staging area unchanged: 85 changed files at the time of the check, 634 changed symbols and 119 affected flows. Aggregate risk is CRITICAL because shared order and notification paths are changed. All changed indexed symbols are in backend source/tests; the other changed files are backend migrations/test configuration and review documents. No frontend or generated instruction/skill changes remain. Graph results can miss framework/reflection calls and truncated execution flows; runtime regression coverage remains necessary.

Higher-impact boundaries include NotificationResponse conversion (CRITICAL), organizer notifications (CRITICAL), pickup completion (HIGH), cancellation mail (HIGH, interface dispatch is a lower-bound graph result) and inventory restoration (HIGH). Acceptance coverage includes existing checkout/cancellation paths, QR/manual pickup, delivery retries, concurrent reservations/cancellation and publication fan-out. Exact counts and reviewed impact summaries are recorded in the validation evidence.

**Validation**

253 tests passed, with 0 failures, 0 errors and 0 skipped tests. The 39 new tests cover all five features, malformed input, races, stock integrity, ownership, transactional rollback and schema upgrade. The unchanged suite includes auth/session security, delivery retries, production-profile wiring, PostgreSQL regression cases and H2 dev startup/authentication.

Reproduce using the isolated harness:

```bash
UITMERCH_TEST_POSTGRES_IMAGE=public.ecr.aws/supabase/postgres:17.6.1.167 \
  backend/scripts/test-postgres.sh -q
```

The harness uses a temporary source copy and PostgreSQL container, then removes its database container. [Validation evidence and source hashes](2026-10-02-backend-feature-validation.json) match the final workspace source. PostgreSQL test lock observation refreshes statistics snapshots as described in [PostgreSQL's monitoring documentation](https://www.postgresql.org/docs/17/monitoring-stats.html).

Swagger is available at `http://localhost:8080/swagger-ui/index.html` with the `dev` profile. In other profiles, explicitly enable `SWAGGER_ENABLED=true` when API documentation is needed. Feature controllers are included through normal Springdoc discovery; frontend rendering of QR codes and campaign/follow/analytics screens remains separate integration work.
