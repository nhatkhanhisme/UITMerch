---
name: controller
description: "Skill for the Controller area of UITMerch. 54 symbols across 30 files."
---

# Controller

54 symbols | 30 files | Cohesion: 69%

## When to Use

- Working with code in `backend/`
- Understanding how register, registerOrganizer, login work
- Modifying controller-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | register, registerOrganizer, login, resendOtp, forgotPassword |
| `backend/src/test/java/com/uitmerch/backend/common/service/RateLimiterServiceTest.java` | isAllowed_withinLimit_returnsTrue, isAllowed_atLimitOnNextAttempt_returnsFalse, isAllowed_differentKeys_haveIndependentLimits, isAllowed_windowExpiry_resetsCounter, isAllowed_zeroAttempts_immediatelyFalse |
| `backend/src/main/java/com/uitmerch/backend/common/model/ApiResponse.java` | success, error, success |
| `backend/src/main/java/com/uitmerch/backend/notification/controller/NotificationController.java` | getUnreadCount, markAllRead, getNotifications |
| `backend/src/main/java/com/uitmerch/backend/notification/repository/NotificationRepository.java` | countByUserIdAndIsReadFalse, markAllReadByUserId, findByUserIdOrderByCreatedAtDesc |
| `backend/src/main/java/com/uitmerch/backend/notification/service/NotificationService.java` | countUnread, markAllRead, getForUser |
| `backend/src/main/java/com/uitmerch/backend/event/repository/EventRepository.java` | findByOrgId, findByStatusIn, findByOrgIdAndStatusIn |
| `backend/src/main/java/com/uitmerch/backend/event/service/EventService.java` | getOwnEvents, getPublicEvents, getPublicEventsByOrg |
| `backend/src/main/java/com/uitmerch/backend/organization/controller/PublicOrganizationController.java` | listOrganizations, getOrgMerch, getOrgEvents |
| `backend/src/main/java/com/uitmerch/backend/admin/controller/AdminController.java` | listUsers, listAllOrders |

## Entry Points

Start here when exploring this area:

- **`register`** (Method) — `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java:36`
- **`registerOrganizer`** (Method) — `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java:57`
- **`login`** (Method) — `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java:93`
- **`resendOtp`** (Method) — `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java:128`
- **`forgotPassword`** (Method) — `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java:145`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `register` | Method | `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | 36 |
| `registerOrganizer` | Method | `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | 57 |
| `login` | Method | `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | 93 |
| `resendOtp` | Method | `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | 128 |
| `forgotPassword` | Method | `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | 145 |
| `health` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/HealthController.java` | 15 |
| `commence` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/JwtAuthenticationEntryPoint.java` | 22 |
| `handleValidation` | Method | `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java` | 22 |
| `success` | Method | `backend/src/main/java/com/uitmerch/backend/common/model/ApiResponse.java` | 34 |
| `error` | Method | `backend/src/main/java/com/uitmerch/backend/common/model/ApiResponse.java` | 61 |
| `isAllowed` | Method | `backend/src/main/java/com/uitmerch/backend/common/service/RateLimiterService.java` | 28 |
| `extractClientIp` | Method | `backend/src/main/java/com/uitmerch/backend/common/util/IpUtil.java` | 44 |
| `getTraceId` | Method | `backend/src/main/java/com/uitmerch/backend/common/util/TraceIdUtil.java` | 39 |
| `getOwnMerchItem` | Method | `backend/src/main/java/com/uitmerch/backend/merch/controller/MerchController.java` | 74 |
| `getUnreadCount` | Method | `backend/src/main/java/com/uitmerch/backend/notification/controller/NotificationController.java` | 53 |
| `markAllRead` | Method | `backend/src/main/java/com/uitmerch/backend/notification/controller/NotificationController.java` | 74 |
| `countByUserIdAndIsReadFalse` | Method | `backend/src/main/java/com/uitmerch/backend/notification/repository/NotificationRepository.java` | 17 |
| `markAllReadByUserId` | Method | `backend/src/main/java/com/uitmerch/backend/notification/repository/NotificationRepository.java` | 19 |
| `countUnread` | Method | `backend/src/main/java/com/uitmerch/backend/notification/service/NotificationService.java` | 40 |
| `markAllRead` | Method | `backend/src/main/java/com/uitmerch/backend/notification/service/NotificationService.java` | 56 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `GuestCheckout → Remove` | cross_community | 7 |
| `Register → Info` | cross_community | 6 |
| `RegisterOrganizer → Info` | cross_community | 6 |
| `Register → ValidationException` | cross_community | 5 |
| `Register → DeleteAllByUser` | cross_community | 5 |
| `Register → GenerateOtpCode` | cross_community | 5 |
| `Register → SendOtp` | cross_community | 5 |
| `RegisterOrganizer → ValidationException` | cross_community | 5 |
| `RegisterOrganizer → DeleteAllByUser` | cross_community | 5 |
| `RegisterOrganizer → GenerateOtpCode` | cross_community | 5 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Service | 16 calls |
| Repository | 1 calls |

## How to Explore

1. `gitnexus_context({name: "register"})` — see callers and callees
2. `gitnexus_query({query: "controller"})` — find related execution flows
3. Read key files listed above for implementation details
