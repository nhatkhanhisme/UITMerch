---
name: service
description: "Skill for the Service area of UITMerch. 446 symbols across 104 files."
---

# Service

446 symbols | 104 files | Cohesion: 67%

## When to Use

- Working with code in `backend/`
- Understanding how error, CreateMerchRequest, UpdateMerchRequest work
- Modifying service-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `backend/src/test/java/com/uitmerch/backend/auth/service/AuthServiceTest.java` | verifyEmail_correctCode_verifiesUser, verifyEmail_wrongCode_incrementsAttemptCount, verifyEmail_expiredOtp_throws, verifyEmail_activeLock_throwsWithMinutesRemaining, verifyEmail_fifthWrongAttempt_triggersLock (+26) |
| `backend/src/main/java/com/uitmerch/backend/order/service/OrderService.java` | getCustomerOrders, getOrgOrders, getOrgOrder, checkInOrder, getPickupScheduleOrders (+21) |
| `backend/src/test/java/com/uitmerch/backend/order/service/OrderServiceTest.java` | getGuestOrderByEmail_matchingEmail_returnsOrder, getGuestOrderByEmail_wrongEmail_throwsResourceNotFound, cancelRequest, cancelCustomerOrder_pendingOrder_cancelsAndRestoresStock, cancelCustomerOrder_confirmedOrder_throwsValidation (+18) |
| `backend/src/main/java/com/uitmerch/backend/merch/service/MerchService.java` | getOwnMerchItem, deleteMerch, findOwnItemOrThrow, getOwnMerch, listByOrganization (+13) |
| `backend/src/test/java/com/uitmerch/backend/merch/service/MerchServiceTest.java` | activeOrg, pendingOrg, savedItem, createMerch_activeOrg_noCategory_succeeds, createMerch_activeOrg_withCategory_resolvesCategorySlug (+13) |
| `backend/src/test/java/com/uitmerch/backend/admin/service/AdminServiceTest.java` | listOrganizations_invalidStatus_throwsValidation, org, listOrganizations_noFilter_returnsAll, updateOrganizationStatus_toSuspended_archivesPublishedMerch, updateOrganizationStatus_toActive_doesNotArchiveMerch (+10) |
| `backend/src/test/java/com/uitmerch/backend/cart/service/CartServiceTest.java` | activeCart, merch, addItem_success_savesCartItem, addItem_duplicateItem_throwsConflict, addItem_outOfStock_throwsValidation (+10) |
| `backend/src/main/java/com/uitmerch/backend/auth/service/AuthService.java` | verifyEmail, resendOtp, register, registerOrganizer, registerWithRole (+7) |
| `backend/src/test/java/com/uitmerch/backend/event/service/EventServiceTest.java` | org, updateEvent_validStatusTransition_succeeds, updateEvent_invalidStatusTransition_throwsValidation, updateEvent_eventNotOwned_throwsResourceNotFound, event (+6) |
| `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java` | findByIdAndOrgId, restoreStock, countByOrgIdsAndStatus, findByOrgId, findByOrgIdAndStatus (+5) |

## Entry Points

Start here when exploring this area:

- **`error`** (Function) — `frontend/src/stores/toastStore.ts:39`
- **`CreateMerchRequest`** (Class) — `backend/src/main/java/com/uitmerch/backend/merch/dto/CreateMerchRequest.java:13`
- **`UpdateMerchRequest`** (Class) — `backend/src/main/java/com/uitmerch/backend/merch/dto/UpdateMerchRequest.java:12`
- **`CancelOrderRequest`** (Class) — `backend/src/main/java/com/uitmerch/backend/order/dto/CancelOrderRequest.java:7`
- **`OtpInfo`** (Class) — `backend/src/main/java/com/uitmerch/backend/auth/controller/DevOtpController.java:45`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `CreateMerchRequest` | Class | `backend/src/main/java/com/uitmerch/backend/merch/dto/CreateMerchRequest.java` | 13 |
| `UpdateMerchRequest` | Class | `backend/src/main/java/com/uitmerch/backend/merch/dto/UpdateMerchRequest.java` | 12 |
| `CancelOrderRequest` | Class | `backend/src/main/java/com/uitmerch/backend/order/dto/CancelOrderRequest.java` | 7 |
| `OtpInfo` | Class | `backend/src/main/java/com/uitmerch/backend/auth/controller/DevOtpController.java` | 45 |
| `VerifyEmailRequest` | Class | `backend/src/main/java/com/uitmerch/backend/auth/dto/VerifyEmailRequest.java` | 7 |
| `GuestOrderItemRequest` | Class | `backend/src/main/java/com/uitmerch/backend/order/dto/GuestOrderItemRequest.java` | 8 |
| `GuestOrderRequest` | Class | `backend/src/main/java/com/uitmerch/backend/order/dto/GuestOrderRequest.java` | 8 |
| `InstantOrderRequest` | Class | `backend/src/main/java/com/uitmerch/backend/order/dto/InstantOrderRequest.java` | 8 |
| `RegisterRequest` | Class | `backend/src/main/java/com/uitmerch/backend/auth/dto/RegisterRequest.java` | 8 |
| `AddCartItemRequest` | Class | `backend/src/main/java/com/uitmerch/backend/cart/dto/AddCartItemRequest.java` | 8 |
| `UpdateEventRequest` | Class | `backend/src/main/java/com/uitmerch/backend/event/dto/UpdateEventRequest.java` | 7 |
| `UpdateOrganizationRequest` | Class | `backend/src/main/java/com/uitmerch/backend/organization/dto/UpdateOrganizationRequest.java` | 6 |
| `UpdateCartItemRequest` | Class | `backend/src/main/java/com/uitmerch/backend/cart/dto/UpdateCartItemRequest.java` | 6 |
| `CheckoutRequest` | Class | `backend/src/main/java/com/uitmerch/backend/cart/dto/CheckoutRequest.java` | 4 |
| `UpdateProfileRequest` | Class | `backend/src/main/java/com/uitmerch/backend/user/dto/UpdateProfileRequest.java` | 6 |
| `ResetPasswordRequest` | Class | `backend/src/main/java/com/uitmerch/backend/auth/dto/ResetPasswordRequest.java` | 7 |
| `LoginRequest` | Class | `backend/src/main/java/com/uitmerch/backend/auth/dto/LoginRequest.java` | 7 |
| `InvalidatedToken` | Class | `backend/src/main/java/com/uitmerch/backend/common/entity/InvalidatedToken.java` | 12 |
| `CreateEventRequest` | Class | `backend/src/main/java/com/uitmerch/backend/event/dto/CreateEventRequest.java` | 7 |
| `CreateOrganizationRequest` | Class | `backend/src/main/java/com/uitmerch/backend/organization/dto/CreateOrganizationRequest.java` | 6 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `Checkout → Remove` | cross_community | 8 |
| `GuestCheckout → Remove` | cross_community | 7 |
| `CreateInstantOrder → Remove` | cross_community | 7 |
| `Register → Info` | cross_community | 6 |
| `RegisterOrganizer → Info` | cross_community | 6 |
| `Checkout → ResourceNotFoundException` | cross_community | 6 |
| `CreatePickupSchedule → Info` | cross_community | 6 |
| `CreatePickupSchedule → Error` | cross_community | 6 |
| `CreatePickupSchedule → StorageException` | cross_community | 6 |
| `VectorSearch → Remove` | cross_community | 6 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Controller | 47 calls |
| Repository | 17 calls |
| Config | 16 calls |
| Security | 9 calls |

## How to Explore

1. `gitnexus_context({name: "error"})` — see callers and callees
2. `gitnexus_query({query: "service"})` — find related execution flows
3. Read key files listed above for implementation details
