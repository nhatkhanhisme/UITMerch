---
name: stores
description: "Skill for the Stores area of UITMerch. 4 symbols across 2 files."
---

# Stores

4 symbols | 2 files | Cohesion: 100%

## When to Use

- Working with code in `frontend/`
- Understanding how setAuthHeader, setSession, clearSession work
- Modifying stores-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `frontend/src/stores/authStore.ts` | setSession, clearSession, onRehydrateStorage |
| `frontend/src/api/auth.ts` | setAuthHeader |

## Entry Points

Start here when exploring this area:

- **`setAuthHeader`** (Function) — `frontend/src/api/auth.ts:91`
- **`setSession`** (Function) — `frontend/src/stores/authStore.ts:24`
- **`clearSession`** (Function) — `frontend/src/stores/authStore.ts:45`
- **`onRehydrateStorage`** (Function) — `frontend/src/stores/authStore.ts:63`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `setAuthHeader` | Function | `frontend/src/api/auth.ts` | 91 |
| `setSession` | Function | `frontend/src/stores/authStore.ts` | 24 |
| `clearSession` | Function | `frontend/src/stores/authStore.ts` | 45 |
| `onRehydrateStorage` | Function | `frontend/src/stores/authStore.ts` | 63 |

## How to Explore

1. `gitnexus_context({name: "setAuthHeader"})` — see callers and callees
2. `gitnexus_query({query: "stores"})` — find related execution flows
3. Read key files listed above for implementation details
