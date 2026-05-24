---
name: exception
description: "Skill for the Exception area of UITMerch. 5 symbols across 2 files."
---

# Exception

5 symbols | 2 files | Cohesion: 80%

## When to Use

- Working with code in `backend/`
- Understanding how handleAppException, handleNoHandlerFound, handleNoResourceFound work
- Modifying exception-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java` | handleAppException, handleNoHandlerFound, handleNoResourceFound, handleGenericException |
| `backend/src/main/java/com/uitmerch/backend/common/model/ApiResponse.java` | error |

## Entry Points

Start here when exploring this area:

- **`handleAppException`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java:38`
- **`handleNoHandlerFound`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java:49`
- **`handleNoResourceFound`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java:60`
- **`handleGenericException`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java:71`
- **`error`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/model/ApiResponse.java:53`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `handleAppException` | Method | `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java` | 38 |
| `handleNoHandlerFound` | Method | `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java` | 49 |
| `handleNoResourceFound` | Method | `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java` | 60 |
| `handleGenericException` | Method | `backend/src/main/java/com/uitmerch/backend/common/exception/GlobalExceptionHandler.java` | 71 |
| `error` | Method | `backend/src/main/java/com/uitmerch/backend/common/model/ApiResponse.java` | 53 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Controller | 2 calls |

## How to Explore

1. `gitnexus_context({name: "handleAppException"})` — see callers and callees
2. `gitnexus_query({query: "exception"})` — find related execution flows
3. Read key files listed above for implementation details
