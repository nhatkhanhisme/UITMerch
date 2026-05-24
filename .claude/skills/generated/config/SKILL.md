---
name: config
description: "Skill for the Config area of UITMerch. 18 symbols across 9 files."
---

# Config

18 symbols | 9 files | Cohesion: 78%

## When to Use

- Working with code in `backend/`
- Understanding how info, run, saveUser work
- Modifying config-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | run, saveUser, saveOrg, saveMerch, saveEvent (+4) |
| `backend/src/main/java/com/uitmerch/backend/common/config/SecurityConfig.java` | corsConfigurationSource, filterChain |
| `backend/src/main/java/com/uitmerch/backend/common/config/OpenApiConfig.java` | openAPI |
| `backend/src/main/java/com/uitmerch/backend/common/config/RequestLoggingInterceptor.java` | afterCompletion |
| `backend/src/main/java/com/uitmerch/backend/common/service/DevEmailService.java` | sendPasswordReset |
| `backend/src/main/java/com/uitmerch/backend/common/util/IpUtil.java` | init |
| `frontend/src/stores/toastStore.ts` | info |
| `backend/src/main/java/com/uitmerch/backend/common/config/TraceIdInterceptor.java` | afterCompletion |
| `backend/src/main/java/com/uitmerch/backend/common/util/TraceIdUtil.java` | clear |

## Entry Points

Start here when exploring this area:

- **`info`** (Function) — `frontend/src/stores/toastStore.ts:42`
- **`run`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java:57`
- **`saveUser`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java:139`
- **`saveOrg`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java:152`
- **`saveMerch`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java:162`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `info` | Function | `frontend/src/stores/toastStore.ts` | 42 |
| `run` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 57 |
| `saveUser` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 139 |
| `saveOrg` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 152 |
| `saveMerch` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 162 |
| `saveEvent` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 174 |
| `attachMerchToEvent` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 186 |
| `saveOrder` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 193 |
| `saveGuestOrder` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 212 |
| `saveOrderItem` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/DevDataInitializer.java` | 233 |
| `openAPI` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/OpenApiConfig.java` | 24 |
| `afterCompletion` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/RequestLoggingInterceptor.java` | 21 |
| `sendPasswordReset` | Method | `backend/src/main/java/com/uitmerch/backend/common/service/DevEmailService.java` | 16 |
| `init` | Method | `backend/src/main/java/com/uitmerch/backend/common/util/IpUtil.java` | 32 |
| `corsConfigurationSource` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/SecurityConfig.java` | 70 |
| `filterChain` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/SecurityConfig.java` | 104 |
| `afterCompletion` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/TraceIdInterceptor.java` | 34 |
| `clear` | Method | `backend/src/main/java/com/uitmerch/backend/common/util/TraceIdUtil.java` | 46 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `Register → Info` | cross_community | 6 |
| `RegisterOrganizer → Info` | cross_community | 6 |
| `CreatePickupSchedule → Info` | cross_community | 6 |
| `ResendOtp → Info` | cross_community | 5 |
| `NotifyOrgOwnerOfCancel → Info` | cross_community | 4 |
| `VerifyEmail → Info` | cross_community | 3 |

## How to Explore

1. `gitnexus_context({name: "info"})` — see callers and callees
2. `gitnexus_query({query: "config"})` — find related execution flows
3. Read key files listed above for implementation details
