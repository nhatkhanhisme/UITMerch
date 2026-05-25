---
name: pages
description: "Skill for the Pages area of UITMerch. 177 symbols across 39 files."
---

# Pages

177 symbols | 39 files | Cohesion: 72%

## When to Use

- Working with code in `frontend/`
- Understanding how getPublicEvents, getPublicMerchList, getPopularMerch work
- Modifying pages-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `frontend/src/pages/OrganizerDashboardPage.tsx` | bustEventsCache, handle, handleSave, formatDate, OrgCancelModal (+19) |
| `frontend/src/pages/OrganizerProfilePage.tsx` | handleOrganizerLogoUpload, handleOrganizerCoverUpload, CalendarIcon, ShieldIcon, CameraIcon (+8) |
| `frontend/src/pages/ProductDetailPage.tsx` | handleBuyNow, handleAddToCart, handleCheckoutSubmit, ShaderBackground, formatPrice (+5) |
| `frontend/src/pages/CustomerProfilePage.tsx` | MailIcon, CalendarIcon, CameraIcon, CustomerProfilePage, updateUser (+5) |
| `frontend/src/pages/AdminDashboardPage.tsx` | ShaderBackground, formatDate, UsersTab, handleRoleChange, handleToggleActive (+4) |
| `frontend/src/pages/CartPage.tsx` | handleCheckout, ShaderBackground, formatPrice, EmptyCart, OrderSuccess (+3) |
| `frontend/src/pages/EventPage.tsx` | timer, fetchAllEvents, ShaderBackground, formatDate, getStatusBadge (+1) |
| `frontend/src/api/order.ts` | createGuestCheckoutOrder, createInstantOrder, getCustomerOrder, updateOrgOrderStatus, checkInOrder (+1) |
| `frontend/src/pages/OrderDetailPage.tsx` | ShaderBackground, formatPrice, formatDateTime, OrderProgressBar, CancelOrderModal (+1) |
| `frontend/src/pages/OrdersPage.tsx` | ShaderBackground, formatPrice, formatDate, StatusBadge, CancelOrderModal (+1) |

## Entry Points

Start here when exploring this area:

- **`getPublicEvents`** (Function) — `frontend/src/api/event.ts:15`
- **`getPublicMerchList`** (Function) — `frontend/src/api/merch.ts:18`
- **`getPopularMerch`** (Function) — `frontend/src/api/merch.ts:26`
- **`getCategories`** (Function) — `frontend/src/api/merch.ts:40`
- **`getPublicOrganizations`** (Function) — `frontend/src/api/organization.ts:15`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `getPublicEvents` | Function | `frontend/src/api/event.ts` | 15 |
| `getPublicMerchList` | Function | `frontend/src/api/merch.ts` | 18 |
| `getPopularMerch` | Function | `frontend/src/api/merch.ts` | 26 |
| `getCategories` | Function | `frontend/src/api/merch.ts` | 40 |
| `getPublicOrganizations` | Function | `frontend/src/api/organization.ts` | 15 |
| `getPublicOrgMerch` | Function | `frontend/src/api/organization.ts` | 30 |
| `cacheGet` | Function | `frontend/src/lib/sessionCache.ts` | 26 |
| `cacheSet` | Function | `frontend/src/lib/sessionCache.ts` | 42 |
| `cacheDelete` | Function | `frontend/src/lib/sessionCache.ts` | 52 |
| `cacheKey` | Function | `frontend/src/lib/sessionCache.ts` | 82 |
| `HomeItem` | Function | `frontend/src/components/home/HomeItem.tsx` | 21 |
| `loadPopular` | Function | `frontend/src/components/home/HomeItem.tsx` | 34 |
| `goToSlide` | Function | `frontend/src/components/home/HomeItem.tsx` | 104 |
| `timer` | Function | `frontend/src/components/home/HomeItem.tsx` | 119 |
| `HomeMoreLink` | Function | `frontend/src/components/home/HomeMoreLink.tsx` | 7 |
| `HomeOrgan` | Function | `frontend/src/components/home/HomeOrgan.tsx` | 22 |
| `loadOrgs` | Function | `frontend/src/components/home/HomeOrgan.tsx` | 30 |
| `timer` | Function | `frontend/src/pages/EventPage.tsx` | 90 |
| `fetchAllEvents` | Function | `frontend/src/pages/EventPage.tsx` | 91 |
| `fetchMeta` | Function | `frontend/src/pages/MerchPage.tsx` | 54 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `HomePage → GetOrganizerProfile` | cross_community | 5 |
| `OrganizationDetailPage → FindOrganizationById` | cross_community | 4 |
| `OrganizerDashboardPage → GetOwnMerch` | cross_community | 4 |
| `OrganizerDashboardPage → GetCategories` | cross_community | 4 |
| `OrganizerDashboardPage → GetOwnEvents` | cross_community | 4 |
| `OrganizerDashboardPage → GetOrgOrders` | cross_community | 4 |
| `MerchPage → BuildKey` | cross_community | 4 |
| `ProductDetailPage → FindProductById` | cross_community | 4 |
| `OrganizerProfilePage → TranslateBackendMessage` | cross_community | 4 |
| `CustomerProfilePage → TranslateBackendMessage` | cross_community | 4 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Api | 22 calls |
| Features | 6 calls |
| Home | 4 calls |
| Ui | 1 calls |

## How to Explore

1. `gitnexus_context({name: "getPublicEvents"})` — see callers and callees
2. `gitnexus_query({query: "pages"})` — find related execution flows
3. Read key files listed above for implementation details
