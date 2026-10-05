# Secure online payment implementation plan — 2026-10-06

This is the next implementation plan, not an enabled payment feature. Keep COD working while adding hosted provider checkout behind a server-side feature flag. No card details, bank credentials or provider secrets enter the frontend or audit logs.

## Decisions already accepted

- Guests verify email before holding stock. Limit 10 units per SKU and 3 pending checkouts per buyer; keep configurable limits and durable checkout actor/idempotency locking.
- COD orders not confirmed by the organizer expire after 48 hours. Confirmed, historical orders without expiry and campaign reservations retain their own rules.
- Browser authentication uses the frontend `/api` reverse proxy and HttpOnly refresh cookies. Access tokens remain in memory.
- Payment amounts come from server-side merchandise/campaign pricing and stored order snapshots. The browser cannot set price, recipient account, payment status or callback destinations.

## Required product/provider decisions before payment code

Choose one supported hosted-checkout provider and obtain separate sandbox/production credentials. Confirm who is the merchant of record and how money reaches each organization. The current cart can create multiple organization orders in one checkout: one platform merchant can settle a single payment across child orders, while separate organization merchants require separate intents. Resolve this before designing reconciliation and payout behavior. Define permitted refund actors and the organizer confirmation rule for a paid order; do not assume successful payment is approval of campaign fulfillment.

Use a provider adapter so business state is independent of the provider SDK. Verify the selected provider's current signature scheme, amount encoding, expiry/retry semantics, server query/refund APIs and sandbox cases against its official documentation. Do not guess these details or use the same key in sandbox and production.

## Data and authorization

Add forward-only Flyway migrations after the operational hardening migrations; preserve historical checksums. New public tables must explicitly enable RLS, deny Supabase browser roles, grant business DML to the runtime role and read-only access to the backup role.

| Record | Required fields and constraints |
|---|---|
| payment_intents | UUID, checkout actor, checkout ID, merchant identity, exact amount/currency, provider, state, expiry and timestamps; immutable price snapshot; unique provider transaction reference |
| payment_intent_orders | Intent/order relationship and immutable attributed amount; prohibit an order from belonging to two active paid attempts |
| payment_attempts | Intent, attempt number, provider request/idempotency reference, hashed request fingerprint, sanitized outcome; stable retries reuse the same provider idempotency reference |
| payment_events | Provider/event ID unique key, verified payload digest and event time, intent reference, processing outcome; minimal retained fields; no credentials or unnecessary PII |
| refunds | Intent/refund ID, exact amount, actor and reason code, provider idempotency reference, state; locked aggregate refunded amount cannot exceed settled amount |

A guest must present the verified checkout credential bound to the actor and order association. A customer must own every order linked to an intent. An organizer may see only its attributed order amount and may refund only if the chosen policy authorizes it. Admin access remains explicit and audited. A UUID or redirect reference alone grants no access.

## State and stock invariants

Intent states: CREATED → AWAITING_PROVIDER → SUCCEEDED / FAILED / EXPIRED; retries are separate attempts. UNKNOWN is a reconciliation state, never assumed failed. Refunds use separate PENDING/SUCCEEDED/FAILED states. Order status and payment status remain separate.

1. Create and price the checkout transactionally under its existing actor lock. Associate all child orders and reserve stock exactly once. Persist the intent and provider idempotency reference before calling the provider.
2. Call hosted checkout outside the database transaction with bounded timeouts. Retry an ambiguous response with the same provider reference or query the provider; never create a second charge because the first timed out.
3. Use a provider-compatible short online-payment reservation window, initially proposed as 15 minutes. This is separate from the accepted COD 48-hour policy. Independent database expiry must distinguish pending COD from pending online intents.
4. A verified successful server event locks the intent and associated orders, matches merchant, transaction reference, currency and exact amount, then commits payment/order/history state once. Delivery effects enter a durable outbox after commit.
5. Expiry and webhook success serialize on the same locks. If a payment succeeds after stock was released, record the received payment and reconcile/refund it; do not silently reacquire unavailable stock or mark the order fulfilled.
6. Campaign deadlines/threshold cancellation and payment/refund transactions require explicit coordination. A failed campaign must create durable refund work for settled reservations, not merely cancel the order.

Do not extend the current COD-only SQL expiry function to online payments without matching these state/locking rules and regression tests.

## Redirects, webhooks and reconciliation

Frontend return pages show pending/confirmed/failed status read from an authorized backend endpoint. Query parameters and browser redirects never mark an order paid. Return URLs are chosen from fixed server configuration.

A provider webhook endpoint has its own authentication: verify the signature over the raw body with the provider's documented canonicalization and constant-time comparison, validate event timestamp/replay window when supported, enforce body limits and merchant/reference/amount/currency binding, and deduplicate by provider event ID before any stock or order changes. Source-IP checks may add defense if documented; they do not replace signature verification. Never require a customer JWT for a provider webhook.

Respond successfully only after the event is durably accepted/committed or durably queued. Use retry-safe processing with a dead-letter path and alerts. Persist sanitized business metadata rather than raw signed bodies with PII. Read provider keys from protected server environment settings, support documented rotation and never include them in URLs/logs.

Reconcile unresolved intents on a schedule independent of a sleeping backend, using an authenticated provider server-query API. Alert on unknown outcomes, duplicate references, amount mismatches, late successful payments and refunds that do not converge. Compare provider settlement exports to the immutable intent/refund ledger; operator fixes must leave an audit trail.

## Implementation order and release gates

1. **Provider contract:** settle merchant/multi-organization and refund decisions, document official provider API semantics, provision sandbox secrets. No live charges.
2. **Ledger and backend adapter:** migrations, authorization, immutable pricing, provider idempotency, signed webhook verification, transitions and outbox/reconciliation. Keep feature flag off.
3. **Hosted checkout UI:** payment-method selection, pending/return/error pages, safe retry messaging, guest credential handling. Continue COD and preserve same-origin auth/CSP; only add provider browser origins if the hosted flow actually requires them.
4. **Adversarial tests:** forged/altered/expired/replayed events; price/currency/merchant tampering; wrong customer/guest/organizer; timeout after provider success; duplicate/delayed/out-of-order webhooks; concurrent expiry/success/cancel/refund; multi-organization atomic settlement; campaign cancellation; repeated refunds; audit/outbox failures and backend restart.
5. **Sandbox end-to-end:** successful/failed/abandoned payment, webhook retry, lost redirect, reconciliation, late success and refund. Verify backups/restore cover the new ledger and new grants remain least-privilege.
6. **Controlled production rollout:** fresh backup, all protected CI gates pass, migrate before deploying, activate for a limited merchant/test cohort and small allowed amount, observe provider reconciliation and alerts, then expand. Switch the feature flag off to stop new intents during rollback while continuing to process verified outstanding webhooks/refunds.

Release is blocked by missing provider decisions, unverified webhook semantics, inconsistent ledger/stock tests, unresolved high-impact vulnerabilities, failed restore verification or a monitoring gap. Operational hardening alone does not make online payment ready for activation.

Security reference: [OWASP Third Party Payment Gateway Integration Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Third_Party_Payment_Gateway_Integration_Cheat_Sheet.html). Provider-specific contracts must be added after selecting the provider.
