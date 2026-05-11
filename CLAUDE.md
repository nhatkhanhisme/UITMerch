# UITMerch Backend — Project Instructions

## Stack
Spring Boot 3.3.x | Java 21 | PostgreSQL | Flyway | JWT (JJWT 0.12.x) | Maven

## Package root
com.uitmerch.backend

## Exception Handling (CRITICAL)
- Custom exceptions are already defined in common/exception/
- Read ALL files in common/exception/ before writing any service code
- Throw those existing exceptions directly from Service — do NOT create new exception classes
- Do NOT create or reference ErrorCode enum
- Do NOT invent new exception classes

## Conventions (MUST follow)
- All responses wrapped in ApiResponse<T>
- Zero business logic in Controller
- @PreAuthorize at method level (not SecurityConfig)
- UUID PK with @UuidGenerator
- @Enumerated(EnumType.STRING) for all enums
- @CreationTimestamp / @UpdateTimestamp
- No bidirectional JPA relationships
- Pagination shape: { data: [...], meta: { page, size, total } }
- All endpoints prefixed /api/v1/
- Exceptions thrown from Service layer only

## Module structure (per domain)
entity/ → repository/ → dto/ → service/ → controller/

## Swagger
springdoc-openapi 2.5.0
Every controller: @Tag, @Operation, @ApiResponse, @SecurityRequirement
