---
name: ui
description: "Skill for the Ui area of UITMerch. 5 symbols across 2 files."
---

# Ui

5 symbols | 2 files | Cohesion: 91%

## When to Use

- Working with code in `frontend/`
- Understanding how useToastStore, ToastContainer work
- Modifying ui-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `frontend/src/components/ui/ToastContainer.tsx` | ToastItem, removeToast, timer, ToastContainer |
| `frontend/src/stores/toastStore.ts` | useToastStore |

## Entry Points

Start here when exploring this area:

- **`useToastStore`** (Function) — `frontend/src/stores/toastStore.ts:17`
- **`ToastContainer`** (Function) — `frontend/src/components/ui/ToastContainer.tsx:59`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `useToastStore` | Function | `frontend/src/stores/toastStore.ts` | 17 |
| `ToastContainer` | Function | `frontend/src/components/ui/ToastContainer.tsx` | 59 |
| `ToastItem` | Function | `frontend/src/components/ui/ToastContainer.tsx` | 21 |
| `removeToast` | Function | `frontend/src/components/ui/ToastContainer.tsx` | 22 |
| `timer` | Function | `frontend/src/components/ui/ToastContainer.tsx` | 26 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `AppRouter → UseToastStore` | cross_community | 4 |
| `AppRouter → RemoveToast` | cross_community | 4 |

## How to Explore

1. `gitnexus_context({name: "useToastStore"})` — see callers and callees
2. `gitnexus_query({query: "ui"})` — find related execution flows
3. Read key files listed above for implementation details
