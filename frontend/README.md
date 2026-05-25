# UITMerch Frontend

React 18 + TypeScript SPA for the UIT merchandise platform.

---

## Quick Start

```bash
npm install
npm run dev       # dev server at http://localhost:5173
npm run build     # type-check + production bundle
npm run preview   # preview production build locally
```

Requires the backend running at `http://localhost:8080` (Docker or dev profile).  
Set `VITE_API_BASE_URL` in `.env.local` if the backend runs elsewhere.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| React 18 + TypeScript | Component-based SPA |
| Vite | Build tool and dev server |
| Tailwind CSS | Utility-first styling |
| React Router v6 | Client-side routing |
| Zustand | Lightweight global state (`authStore`, `cartStore`) |
| React Query (`@tanstack/react-query`) | Server state, caching, background refetch |
| Axios | HTTP client — `src/api/client.ts` points at `VITE_API_BASE_URL` |
| Three.js / `@react-three/fiber` | 3D animations on the landing page |
| EventSource (SSE) | Real-time in-app notifications for customers and organizers |

---

## Project Structure

```
src/
├── api/            # Axios wrappers per domain (merch, orders, notifications, …)
├── components/     # Shared UI components (TopNavBar, modals, cards)
│   └── home/       # Landing-page sections (Hero, FeaturedMerch, OrgGrid, …)
├── lib/            # Utilities (sessionCache, formatters)
├── pages/          # Route-level page components (MerchPage, CartPage, OrdersPage, …)
├── stores/         # Zustand stores (authStore, cartStore)
└── types/          # Shared TypeScript types (NotificationResponse, OrderResponse, …)
```

---

## State Management

- **`authStore`** — current user (id, email, role, token). Persisted to `localStorage`.
- **`cartStore`** — cart item count badge. Synced with backend on login.
- **React Query** — server state for merch lists, orders, notifications. Cache invalidated on mutations.

---

## Real-time Notifications

Both customers and organizers receive real-time push via SSE:

- **Customer** — `GET /api/v1/customer/notifications/stream` — notified on ORDER_PLACED (checkout confirmation), ORDER_READY (pickup schedule), etc.
- **Organizer** — `GET /api/v1/organizer/notifications/stream` — notified on NEW_ORDER and ORDER_CANCELLED.

The `EventSource` connection is opened in `TopNavBar` after login and closed on logout. Unread count badge updates in real time; the notification panel fetches persisted history from the DB on open.

---

## Key Pages

| Route | Page | Role |
|---|---|---|
| `/` | Home | Public |
| `/merch` | Merch store (search, filter, sort) | Public |
| `/merch/:id` | Merch detail + AI visual search suggestions | Public |
| `/organization` | Organization grid | Public |
| `/event` | Event listing | Public |
| `/auth` | Login / Register / OTP / Reset password | Public |
| `/cart` | Cart and checkout | Customer |
| `/orders` | Order history + status tracking | Customer |
| `/organizer` | Organizer dashboard (orders, merchs, events, pickup) | Organizer |
| `/admin` | Admin panel (users, orgs) | Admin |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend base URL |
