---
name: repository
description: "Skill for the Repository area of UITMerch. 16 symbols across 9 files."
---

# Repository

16 symbols | 9 files | Cohesion: 59%

## When to Use

- Working with code in `backend/`
- Understanding how findByStatus, findByStatusAndNameContainingIgnoreCase, findByStatusAndCategoryId work
- Modifying repository-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java` | findByStatus, findByStatusAndNameContainingIgnoreCase, findByStatusAndCategoryId, findByStatusAndCategoryIdAndNameContainingIgnoreCase, deductStock |
| `backend/src/test/java/com/uitmerch/backend/merch/repository/MerchItemRepositoryTest.java` | savedMerch, deductStock_sufficientStock_returnsOneAndDeducts, deductStock_insufficientStock_returnsZeroAndLeavesSame, deductStock_concurrent_neverGoesNegative |
| `backend/src/main/java/com/uitmerch/backend/merch/service/MerchService.java` | listPublished |
| `backend/src/main/java/com/uitmerch/backend/event/repository/EventMerchRepository.java` | findByEventId |
| `backend/src/main/java/com/uitmerch/backend/event/service/EventService.java` | fetchMerchForEvent |
| `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchImageRepository.java` | findByMerchIdInOrderByPosition |
| `backend/src/test/java/com/uitmerch/backend/event/service/EventServiceTest.java` | getPublicEvent_publishedEvent_returnsResponse |
| `backend/src/main/java/com/uitmerch/backend/auth/repository/OtpTokenRepository.java` | deleteExpiredBefore |
| `backend/src/main/java/com/uitmerch/backend/auth/service/AuthService.java` | purgeExpiredOtps |

## Entry Points

Start here when exploring this area:

- **`findByStatus`** (Method) — `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java:19`
- **`findByStatusAndNameContainingIgnoreCase`** (Method) — `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java:25`
- **`findByStatusAndCategoryId`** (Method) — `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java:27`
- **`findByStatusAndCategoryIdAndNameContainingIgnoreCase`** (Method) — `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java:29`
- **`listPublished`** (Method) — `backend/src/main/java/com/uitmerch/backend/merch/service/MerchService.java:152`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `findByStatus` | Method | `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java` | 19 |
| `findByStatusAndNameContainingIgnoreCase` | Method | `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java` | 25 |
| `findByStatusAndCategoryId` | Method | `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java` | 27 |
| `findByStatusAndCategoryIdAndNameContainingIgnoreCase` | Method | `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java` | 29 |
| `listPublished` | Method | `backend/src/main/java/com/uitmerch/backend/merch/service/MerchService.java` | 152 |
| `deductStock` | Method | `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchItemRepository.java` | 49 |
| `findByEventId` | Method | `backend/src/main/java/com/uitmerch/backend/event/repository/EventMerchRepository.java` | 14 |
| `fetchMerchForEvent` | Method | `backend/src/main/java/com/uitmerch/backend/event/service/EventService.java` | 184 |
| `findByMerchIdInOrderByPosition` | Method | `backend/src/main/java/com/uitmerch/backend/merch/repository/MerchImageRepository.java` | 15 |
| `deleteExpiredBefore` | Method | `backend/src/main/java/com/uitmerch/backend/auth/repository/OtpTokenRepository.java` | 21 |
| `purgeExpiredOtps` | Method | `backend/src/main/java/com/uitmerch/backend/auth/service/AuthService.java` | 313 |
| `savedMerch` | Method | `backend/src/test/java/com/uitmerch/backend/merch/repository/MerchItemRepositoryTest.java` | 58 |
| `deductStock_sufficientStock_returnsOneAndDeducts` | Method | `backend/src/test/java/com/uitmerch/backend/merch/repository/MerchItemRepositoryTest.java` | 70 |
| `deductStock_insufficientStock_returnsZeroAndLeavesSame` | Method | `backend/src/test/java/com/uitmerch/backend/merch/repository/MerchItemRepositoryTest.java` | 80 |
| `deductStock_concurrent_neverGoesNegative` | Method | `backend/src/test/java/com/uitmerch/backend/merch/repository/MerchItemRepositoryTest.java` | 90 |
| `getPublicEvent_publishedEvent_returnsResponse` | Method | `backend/src/test/java/com/uitmerch/backend/event/service/EventServiceTest.java` | 163 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `Checkout → DeductStock` | cross_community | 4 |
| `ListMerch → FindAll` | cross_community | 4 |
| `GetOwnMerch → FindByMerchIdInOrderByPosition` | cross_community | 4 |
| `GetOrgMerch → FindByMerchIdInOrderByPosition` | cross_community | 4 |
| `UpdateItem → FindByMerchIdInOrderByPosition` | cross_community | 4 |
| `GetCart → FindByMerchIdInOrderByPosition` | cross_community | 4 |
| `GetOwnEvent → FindByEventId` | cross_community | 4 |
| `GetOwnEvent → FindAll` | cross_community | 4 |
| `GetOwnEvent → FindByMerchIdInOrderByPosition` | cross_community | 4 |
| `GetOwnEvent → From` | cross_community | 4 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Service | 9 calls |

## How to Explore

1. `gitnexus_context({name: "findByStatus"})` — see callers and callees
2. `gitnexus_query({query: "repository"})` — find related execution flows
3. Read key files listed above for implementation details
