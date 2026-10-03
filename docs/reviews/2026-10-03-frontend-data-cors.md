# Frontend data loading: CORS fix — 3 October 2026

The local frontend at `http://localhost:5173` could not load backend data even though Docker was healthy. Its served Vite API base was correctly set to `http://localhost:8080`; the failing boundary was backend CORS configuration.

The running container's `APP_CORS_ALLOWED_ORIGINS` contained only `https://uitmerch.vercel.app/`. This replaced the default localhost allowlist. Requests with `Origin: http://localhost:5173` returned 403 without `Access-Control-Allow-Origin`, and an actual Chromium visit to `/merch` reproduced browser CORS errors. The trailing `/` also prevented an exact match with the Vercel browser origin `https://uitmerch.vercel.app`.

## Fix applied

The ignored local `backend/.env` now lists these exact origins:

```dotenv
APP_CORS_ALLOWED_ORIGINS=https://uitmerch.vercel.app,http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173
```

Other variables are preserved, including both empty Gemini keys. Compose recreated the backend container using the existing image, loading the changed runtime environment. All 17 audited variables match the container. No application function or security policy implementation changed.

The committed `.env.example` now includes local development origins and instructs deployers to append deployed origins. [Environment setup](../../backend/ENV_SETUP.md) explains exact origin matching, the difference between localhost and 127.0.0.1, and container recreation after `.env` changes. The ignored file containing real credentials is not committed.

## Verification

**Ten CORS checks passed:** GET and preflight OPTIONS requests succeed with matching allow-origin headers for localhost:5173, localhost:3000, 127.0.0.1:5173 and the configured Vercel origin. An unapproved origin is still rejected with 403 for both methods. Docker remains healthy with zero restarts.

Actual Chromium visits use the running Vite server and Docker API, with fresh browser contexts and **no API mocks**:

| Frontend page | Rendered items | API status | Browser CORS errors |
| --- | --- | --- | --- |
| `/merch` | 16 products | 200 for merchandise, popular merchandise, categories and organizations | 0 |
| `/organization` | 16 organization cards | 200 | 0 |
| `/events` | 7 events | 200 for events and organizations | 0 |

The [sanitized validation artifact](2026-10-03-frontend-data-cors.json) contains statuses and aggregate counts, without credentials or application response bodies. A page reload is sufficient for the already running local frontend.

The preceding [Docker validation](2026-10-03-docker-validation.md) tested API requests without browser origins and browser workflows with route mocks. Those checks missed this integration issue. This follow-up verifies the actual browser-to-backend connection. The hosted Vercel origin is checked with HTTP headers; no hosted frontend settings or remote deployment are changed. For a deployed frontend, `VITE_API_BASE_URL` must point to an externally reachable backend, rather than localhost.
