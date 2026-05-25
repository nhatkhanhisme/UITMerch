---
name: api
description: "Skill for the Api area of UITMerch. 65 symbols across 22 files."
---

# Api

65 symbols | 22 files | Cohesion: 74%

## When to Use

- Working with code in `frontend/`
- Understanding how verifyEmail, resendOtp, forgotPassword work
- Modifying api-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `frontend/src/api/auth.ts` | verifyEmail, resendOtp, forgotPassword, resetPassword, login (+4) |
| `frontend/src/pages/OrganizerDashboardPage.tsx` | load, handleCancel, handleCreate, handleImageFileUpload, handleCoverUpload (+3) |
| `frontend/src/api/order.ts` | getOrgOrders, cancelOrgOrder, createPickupSchedule, getPickupSchedules, getCustomerOrders (+1) |
| `frontend/src/api/storage.ts` | createFileId, buildFilePath, uploadImage, uploadAvatarImage, uploadMerchImage (+1) |
| `frontend/src/pages/AuthPage.tsx` | AuthPage, handleModeChange, handleAccountTypeChange, setSession, handleSubmit |
| `frontend/src/api/event.ts` | getOwnEvents, createEvent, updateEvent, attachMerchToEvent, getPublicEvent |
| `frontend/src/api/merch.ts` | getOwnMerch, createMerch, updateMerch, getPublicMerchDetail, searchMerchByImage |
| `frontend/src/api/admin.ts` | adminListUsers, adminListOrganizations, adminListOrders |
| `frontend/src/api/organization.ts` | createOrganization, getPublicOrganizationDetail |
| `frontend/src/pages/ProductDetailPage.tsx` | fetchProduct, handleWishlistToggle |

## Entry Points

Start here when exploring this area:

- **`verifyEmail`** (Function) — `frontend/src/api/auth.ts:67`
- **`resendOtp`** (Function) — `frontend/src/api/auth.ts:100`
- **`forgotPassword`** (Function) — `frontend/src/api/auth.ts:108`
- **`resetPassword`** (Function) — `frontend/src/api/auth.ts:116`
- **`AmbientBackgroundGradients`** (Function) — `frontend/src/components/home/AmbientBackgroundGradients.tsx:0`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `verifyEmail` | Function | `frontend/src/api/auth.ts` | 67 |
| `resendOtp` | Function | `frontend/src/api/auth.ts` | 100 |
| `forgotPassword` | Function | `frontend/src/api/auth.ts` | 108 |
| `resetPassword` | Function | `frontend/src/api/auth.ts` | 116 |
| `AmbientBackgroundGradients` | Function | `frontend/src/components/home/AmbientBackgroundGradients.tsx` | 0 |
| `Button` | Function | `frontend/src/components/ui/Button.tsx` | 24 |
| `AuthPage` | Function | `frontend/src/pages/AuthPage.tsx` | 44 |
| `handleModeChange` | Function | `frontend/src/pages/AuthPage.tsx` | 79 |
| `handleAccountTypeChange` | Function | `frontend/src/pages/AuthPage.tsx` | 85 |
| `getOwnEvents` | Function | `frontend/src/api/event.ts` | 37 |
| `getOwnMerch` | Function | `frontend/src/api/merch.ts` | 54 |
| `getOrgOrders` | Function | `frontend/src/api/order.ts` | 67 |
| `cancelOrgOrder` | Function | `frontend/src/api/order.ts` | 94 |
| `createPickupSchedule` | Function | `frontend/src/api/order.ts` | 113 |
| `getPickupSchedules` | Function | `frontend/src/api/order.ts` | 121 |
| `uploadAvatarImage` | Function | `frontend/src/api/storage.ts` | 56 |
| `uploadMerchImage` | Function | `frontend/src/api/storage.ts` | 69 |
| `uploadEventImage` | Function | `frontend/src/api/storage.ts` | 74 |
| `handleAvatarUpload` | Function | `frontend/src/pages/CustomerProfilePage.tsx` | 174 |
| `createEvent` | Function | `frontend/src/api/event.ts` | 45 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `OrganizerDashboardPage → GetOwnMerch` | cross_community | 4 |
| `OrganizerDashboardPage → GetCategories` | cross_community | 4 |
| `OrganizerDashboardPage → GetOwnEvents` | cross_community | 4 |
| `OrganizerDashboardPage → GetOrgOrders` | cross_community | 4 |
| `ProductDetailPage → FindProductById` | cross_community | 4 |
| `HandleAvatarUpload → CreateFileId` | intra_community | 4 |
| `HandleOrganizerLogoUpload → CreateFileId` | cross_community | 4 |
| `HandleOrganizerCoverUpload → CreateFileId` | cross_community | 4 |
| `AdminDashboardPage → AdminListUsers` | cross_community | 4 |
| `AdminDashboardPage → AdminListOrganizations` | cross_community | 4 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Pages | 17 calls |

## How to Explore

1. `gitnexus_context({name: "verifyEmail"})` — see callers and callees
2. `gitnexus_query({query: "api"})` — find related execution flows
3. Read key files listed above for implementation details
