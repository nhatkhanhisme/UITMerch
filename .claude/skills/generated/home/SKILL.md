---
name: home
description: "Skill for the Home area of UITMerch. 40 symbols across 14 files."
---

# Home

40 symbols | 14 files | Cohesion: 86%

## When to Use

- Working with code in `frontend/`
- Understanding how HomeEnd, HomeFixedChrome, updateScale work
- Modifying home-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `frontend/src/components/home/TopNavBar.tsx` | TopNavBar, normalizePath, handleMarkRead, linkClassName, updateUser (+7) |
| `frontend/src/components/home/FluidGlass.tsx` | useModel, ModeWrapper, Lens, BackgroundPlane, Cube (+2) |
| `frontend/src/pages/HomePage.tsx` | HomePage, updateHash, snapToIndex, onWheel |
| `frontend/src/components/home/HomeHero.tsx` | HeroAnniversaryLogo, HomeHero, updateScale |
| `frontend/src/components/home/HomeEnd.tsx` | InfoList, HomeEnd |
| `frontend/src/components/home/HomeFixedChrome.tsx` | HomeFixedChrome, updateScale |
| `frontend/src/api/notifications.ts` | getUnreadCount, markNotificationRead |
| `frontend/src/hooks/useNotificationStream.ts` | useNotificationStream, connect |
| `frontend/src/components/home/SlideBar.tsx` | SlideBar |
| `frontend/src/components/home/ScrollDownButton.tsx` | ScrollDownButton |

## Entry Points

Start here when exploring this area:

- **`HomeEnd`** (Function) — `frontend/src/components/home/HomeEnd.tsx:27`
- **`HomeFixedChrome`** (Function) — `frontend/src/components/home/HomeFixedChrome.tsx:8`
- **`updateScale`** (Function) — `frontend/src/components/home/HomeFixedChrome.tsx:15`
- **`SlideBar`** (Function) — `frontend/src/components/home/SlideBar.tsx:56`
- **`HomePage`** (Function) — `frontend/src/pages/HomePage.tsx:9`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `HomeEnd` | Function | `frontend/src/components/home/HomeEnd.tsx` | 27 |
| `HomeFixedChrome` | Function | `frontend/src/components/home/HomeFixedChrome.tsx` | 8 |
| `updateScale` | Function | `frontend/src/components/home/HomeFixedChrome.tsx` | 15 |
| `SlideBar` | Function | `frontend/src/components/home/SlideBar.tsx` | 56 |
| `HomePage` | Function | `frontend/src/pages/HomePage.tsx` | 9 |
| `updateHash` | Function | `frontend/src/pages/HomePage.tsx` | 40 |
| `snapToIndex` | Function | `frontend/src/pages/HomePage.tsx` | 49 |
| `onWheel` | Function | `frontend/src/pages/HomePage.tsx` | 100 |
| `getUnreadCount` | Function | `frontend/src/api/notifications.ts` | 11 |
| `markNotificationRead` | Function | `frontend/src/api/notifications.ts` | 18 |
| `useNotificationStream` | Function | `frontend/src/hooks/useNotificationStream.ts` | 9 |
| `connect` | Function | `frontend/src/hooks/useNotificationStream.ts` | 26 |
| `TopNavBar` | Function | `frontend/src/components/home/TopNavBar.tsx` | 38 |
| `normalizePath` | Function | `frontend/src/components/home/TopNavBar.tsx` | 57 |
| `handleMarkRead` | Function | `frontend/src/components/home/TopNavBar.tsx` | 231 |
| `linkClassName` | Function | `frontend/src/components/home/TopNavBar.tsx` | 274 |
| `HomeHero` | Function | `frontend/src/components/home/HomeHero.tsx` | 141 |
| `updateScale` | Function | `frontend/src/components/home/HomeHero.tsx` | 147 |
| `ScrollDownButton` | Function | `frontend/src/components/home/ScrollDownButton.tsx` | 7 |
| `BackgroundEffect` | Function | `frontend/src/components/ui/BackgroundEffect.tsx` | 0 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `HomePage → GetCustomerProfile` | cross_community | 5 |
| `HomePage → UpdateUser` | cross_community | 5 |
| `HomePage → GetOrganizerProfile` | cross_community | 5 |
| `HomePage → Connect` | cross_community | 5 |
| `HomePage → NormalizePath` | cross_community | 4 |
| `HomePage → GetUnreadCount` | cross_community | 4 |
| `AppRouter → UpdateScale` | cross_community | 4 |
| `AppRouter → SlideBar` | cross_community | 4 |
| `HomePage → UpdateScale` | cross_community | 3 |
| `HomePage → BackgroundEffect` | cross_community | 3 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Pages | 3 calls |
| Api | 1 calls |

## How to Explore

1. `gitnexus_context({name: "HomeEnd"})` — see callers and callees
2. `gitnexus_query({query: "home"})` — find related execution flows
3. Read key files listed above for implementation details
