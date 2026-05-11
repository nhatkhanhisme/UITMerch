# UITMerch Backend — Project Instructions

## Stack
Spring Boot 3.3.x | Java 21 | PostgreSQL | Flyway | JWT (JJWT 0.12.x) | Maven

## Package root
com.uitmerch.backend

## Exception Handling
- Read ALL files in common/exception/ before writing any service code
- Reuse existing exceptions whenever they fit
- Create a new exception class ONLY IF no existing one semantically fits
- New exceptions go in common/exception/, extend AppException, follow the same pattern
- Throw from Service layer only — never from Controller
- GlobalExceptionHandler must handle every exception type — add a handler if creating a new exception

## Response Shape (CRITICAL)
Every response MUST use ApiResponse<T> with this exact structure:
{
  "success": true | false,
  "message":  "Human-readable string — safe to display directly in frontend UI",
  "data":     <T> | null,
  "meta":     { "page": 0, "size": 10, "total": 100 } | null,
  "traceId":  "uuid — unique per request, use for support/log lookup"
}

Rules:
- message must be human-readable and frontend-displayable (no stack traces, no Java class names)
- success: true  → HTTP 2xx, data contains the payload, meta populated if paginated
- success: false → HTTP 4xx/5xx, data is null, message explains what went wrong
- meta is null on non-paginated responses
- traceId is auto-generated — never pass manually

## HTTP Status Semantics (follow strictly)
200 OK            → successful GET, PATCH
201 Created       → successful POST that creates a resource
204 No Content    → successful DELETE
400 Bad Request   → validation failure, malformed request
401 Unauthorized  → missing or invalid JWT
403 Forbidden     → authenticated but not allowed
404 Not Found     → resource does not exist
409 Conflict      → duplicate resource, business rule violation
422 Unprocessable → semantically invalid request (e.g. insufficient stock)

## Validation Error Shape
400 responses from @Valid failures must return field-level errors:
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Email must be valid",
    "password": "Password must be at least 8 characters"
  },
  "meta": null,
  "traceId": "..."
}

## Conventions (MUST follow)
- All responses wrapped in ApiResponse<T>
- Zero business logic in Controller
- @PreAuthorize at method level (not SecurityConfig)
- UUID PK with @UuidGenerator
- @Enumerated(EnumType.STRING) for all enums
- @CreationTimestamp / @UpdateTimestamp
- No bidirectional JPA relationships
- All endpoints prefixed /api/v1/
- Paginated endpoints accept ?page=0&size=10&sort=createdAt,desc

## Module structure (per domain)
entity/ → repository/ → dto/ → service/ → controller/

## Swagger
springdoc-openapi 2.5.0
Every controller: @Tag, @Operation, @ApiResponse, @SecurityRequirement