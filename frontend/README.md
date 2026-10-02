# UITMerch Frontend

React 18 + TypeScript + Vite SPA. Backend must include migrations through V41 and the campaign context APIs described in [backend README](../backend/README.md).

## Local development

```bash
npm ci
cp .env.example .env.local
npm run dev
```

Open `http://localhost:5173`. Set `VITE_API_BASE_URL` to your backend origin (default `http://localhost:8080`, without `/api/v1`). Optional Supabase public URL/anon key and existing bucket names enable image uploads. Missing storage configuration leaves browsing available and shows an error when uploading. Never put server credentials in `VITE_*`.

## Routes and features

| Route | Access | Behavior |
|---|---|---|
| `/merch`, `/merch/:id` | Public | Catalog, ordinary checkout, restock CTA and active campaign link |
| `/organization`, `/organization/:id` | Public | Organization discovery and customer follow |
| `/events`, `/event/:id` | Public | Events and event details |
| `/auth` | Public | Login, registration, OTP and reset |
| `/campaigns`, `/campaigns/:id` | Public | Campaign discovery; customers reserve real variants |
| `/guest-orders` | Guest | Email/receipt tracking and emailed pickup receipt exchange |
| `/cart`, `/wishlist`, `/orders`, `/orders/:id` | Customer | Checkout, order history, campaign context and READY pickup QR |
| `/restock-subscriptions`, `/following`, `/reservations` | Customer | Subscriptions, preferences and campaign reservations |
| `/organizer?orgId=…&tab=analytics` | Organizer | Date-filtered metrics and tables |
| `/organizer?orgId=…&tab=campaigns` | Organizer | Create, inspect and cancel campaigns |
| `/organizer?orgId=…&tab=scanner` | Organizer | Camera/manual credential verification, then explicit check-in |
| `/admin` | Admin | Organization approvals and user roles |

Primary navigation contains Home, Merch, Organizations and Events. Preorder campaigns have a dedicated spotlight below the homepage introduction and a compact banner in the catalog. Customer reservations remain accessible through the account menu. See the [preorder placement review](../docs/reviews/2026-10-02-preorder-placement.md) for screenshots and validation.

## State and retry behavior

`api/client.ts` coordinates one refresh per tab and uses Web Locks across tabs, with a storage lease fallback. Logout/account changes clear private queries, session cache, cart and reservation intents. Failed authorization clears the current session; network failure preserves it. Protected requests retry once after refresh; ambiguous checkout/network failures are not automatically replayed.

React Query keys include user/org for the new feature data. Older catalog/dashboard views still use existing state/cache. Notification REST records are authoritative; SSE reconnect reloads records/counts, follows rotated tokens and invalidates matching domains. Opening the panel does not mark all notifications read.

Reservation intents freeze request ID and payload in user-scoped sessionStorage for an explicit retry after ambiguous failure or reload. Pickup tokens and guest receipt credentials stay in component memory; receipt query parameters are stripped after initialization. Issuing a new pickup token invalidates the old one on the server. Camera denial leaves manual input available.

## Tests and production build

```bash
npm test
npm run build
npx playwright install --with-deps chromium
npm run test:e2e
```

Vitest covers auth races, adapters, SSE lifecycle and component behavior. Playwright starts Vite on port 5189 and intercepts API calls with contract fixtures; it does not send test orders to production. To use an already installed Chromium, set `PLAYWRIGHT_CHROMIUM_EXECUTABLE` to its executable path. See [validation](../docs/reviews/2026-10-02-frontend-validation.md) for results and integration limits.

## Vercel

Use project root `frontend`, build `npm run build`, output `dist`, install `npm ci`. `vercel.json` rewrites deep links to the SPA. Configure `VITE_API_BASE_URL` per production/preview environment before building. Backend CORS must list each allowed frontend origin. Deploy the compatible backend before enabling these frontend routes; Vercel Git integration runs independently of the Render workflow.
