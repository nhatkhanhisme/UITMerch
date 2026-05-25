---
name: security
description: "Skill for the Security area of UITMerch. 32 symbols across 6 files."
---

# Security

32 symbols | 6 files | Cohesion: 55%

## When to Use

- Working with code in `backend/`
- Understanding how JwtTokenProvider, refresh, refreshToken work
- Modifying security-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | generateAccessToken, getExpiryFromToken, generateToken, getAllClaims, getSigningKey (+8) |
| `backend/src/test/java/com/uitmerch/backend/common/security/JwtTokenProviderTest.java` | getExpiryFromToken_isInFuture, generateAccessToken_claimsRoundtrip, generateAccessToken_validateToken_returnsTrue, validateToken_tamperedSignature_returnsFalse, validateToken_randomString_returnsFalse (+6) |
| `backend/src/test/java/com/uitmerch/backend/auth/service/AuthServiceTest.java` | refreshToken_validRefreshToken_returnsNewTokens, refreshToken_nullToken_throws, refreshToken_accessTokenPassedAsRefresh_throws, refreshToken_blacklistedToken_throws |
| `backend/src/main/java/com/uitmerch/backend/common/config/JwtAuthenticationFilter.java` | doFilterInternal, extractToken |
| `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | refresh |
| `backend/src/main/java/com/uitmerch/backend/auth/service/AuthService.java` | refreshToken |

## Entry Points

Start here when exploring this area:

- **`JwtTokenProvider`** (Class) — `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java:23`
- **`refresh`** (Method) — `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java:113`
- **`refreshToken`** (Method) — `backend/src/main/java/com/uitmerch/backend/auth/service/AuthService.java:143`
- **`generateAccessToken`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java:55`
- **`getExpiryFromToken`** (Method) — `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java:116`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `JwtTokenProvider` | Class | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 23 |
| `refresh` | Method | `backend/src/main/java/com/uitmerch/backend/auth/controller/AuthController.java` | 113 |
| `refreshToken` | Method | `backend/src/main/java/com/uitmerch/backend/auth/service/AuthService.java` | 143 |
| `generateAccessToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 55 |
| `getExpiryFromToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 116 |
| `generateToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 129 |
| `getAllClaims` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 146 |
| `getSigningKey` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 154 |
| `doFilterInternal` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/JwtAuthenticationFilter.java` | 44 |
| `extractToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/config/JwtAuthenticationFilter.java` | 94 |
| `getUserIdFromToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 104 |
| `getEmailFromToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 108 |
| `getRoleFromToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 112 |
| `getClaim` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 142 |
| `validateToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 74 |
| `generateRefreshToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 67 |
| `validateAsRefreshToken` | Method | `backend/src/main/java/com/uitmerch/backend/common/security/JwtTokenProvider.java` | 99 |
| `refreshToken_validRefreshToken_returnsNewTokens` | Method | `backend/src/test/java/com/uitmerch/backend/auth/service/AuthServiceTest.java` | 411 |
| `refreshToken_nullToken_throws` | Method | `backend/src/test/java/com/uitmerch/backend/auth/service/AuthServiceTest.java` | 452 |
| `getExpiryFromToken_isInFuture` | Method | `backend/src/test/java/com/uitmerch/backend/common/security/JwtTokenProviderTest.java` | 92 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `Refresh → GetSigningKey` | cross_community | 6 |
| `Login → GetSigningKey` | cross_community | 5 |
| `Logout → GetSigningKey` | cross_community | 5 |
| `DoFilterInternal → GetSigningKey` | cross_community | 5 |
| `Refresh → Sha256` | cross_community | 4 |
| `Refresh → AuthenticationException` | intra_community | 3 |
| `Refresh → GetTraceId` | cross_community | 3 |
| `DoFilterInternal → Sha256` | cross_community | 3 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Service | 6 calls |
| Controller | 1 calls |

## How to Explore

1. `gitnexus_context({name: "JwtTokenProvider"})` — see callers and callees
2. `gitnexus_query({query: "security"})` — find related execution flows
3. Read key files listed above for implementation details
