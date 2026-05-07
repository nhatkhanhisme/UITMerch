/**
 * API RESPONSE FORMATS DOCUMENTATION
 * ===================================
 * 
 * All UITMerch API responses follow a standardized envelope structure.
 * This file documents all response and error formats.
 */

// ============================================================================
// 1. SUCCESSFUL SINGLE OBJECT RESPONSE (POST/PATCH/DELETE)
// ============================================================================
// Endpoint: POST /api/v1/auth/login
// Status: 200 OK or 201 Created
{
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "CUSTOMER",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400000
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 2. SUCCESSFUL LIST RESPONSE WITH PAGINATION (GET)
// ============================================================================
// Endpoint: GET /api/v1/public/merch?page=0&pageSize=20
// Status: 200 OK
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "UITMerch Hoodie",
      "meaningText": "Represents the vibrant IT community at UIT",
      "price": 150000.00,
      "stockQuantity": 50,
      "isPreorder": false,
      "imageUrl": "https://storage.example.com/hoodie.jpg",
      "organization": {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "name": "IT Club"
      }
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "name": "T-Shirt",
      "meaningText": "Simple and elegant",
      "price": 50000.00,
      "stockQuantity": 0,
      "isPreorder": true,
      "imageUrl": "https://storage.example.com/tshirt.jpg",
      "organization": {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "name": "IT Club"
      }
    }
  ],
  "meta": {
    "page": 0,
    "pageSize": 20,
    "totalElements": 42,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 3. VALIDATION ERROR RESPONSE (SYS_001)
// ============================================================================
// Endpoint: POST /api/v1/auth/register
// Status: 400 Bad Request
// Triggered when: @Valid MethodArgumentNotValidException
{
  "data": {
    "errorCode": "SYS_001",
    "message": "Request validation failed",
    "timestamp": "2026-05-06T10:30:00.000Z",
    "fieldErrors": [
      {
        "field": "email",
        "message": "must be a valid email address",
        "rejectedValue": "invalid-email"
      },
      {
        "field": "password",
        "message": "must be at least 8 characters",
        "rejectedValue": "short"
      },
      {
        "field": "fullName",
        "message": "must not be blank",
        "rejectedValue": ""
      }
    ]
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 4. VALIDATION ERROR RESPONSE (VALIDATION_ERROR)
// ============================================================================
// Endpoint: POST /api/v1/auth/register
// Status: 400 Bad Request
// Triggered when: Custom business logic validation fails
{
  "data": {
    "errorCode": "VALIDATION_ERROR",
    "message": "Email must contain 8+ chars, uppercase, lowercase, number",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 5. CONFLICT ERROR RESPONSE (CONFLICT)
// ============================================================================
// Endpoint: POST /api/v1/auth/register
// Status: 409 Conflict
// Triggered when: Email already exists (BR01 - email is globally unique)
{
  "data": {
    "errorCode": "CONFLICT",
    "message": "Email already exists: user@example.com",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 6. AUTHENTICATION ERROR RESPONSE (AUTHENTICATION_FAILED)
// ============================================================================
// Endpoint: POST /api/v1/auth/login
// Status: 401 Unauthorized
// Triggered when: Invalid email or password
{
  "data": {
    "errorCode": "AUTHENTICATION_FAILED",
    "message": "Invalid email or password",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 7. AUTHENTICATION ERROR RESPONSE (JWT - No Authorization Header)
// ============================================================================
// Endpoint: GET /api/v1/customer/carts
// Status: 401 Unauthorized
// Triggered when: No Authorization header or invalid JWT
{
  "data": {
    "errorCode": "AUTHENTICATION_FAILED",
    "message": "Unauthorized access - please provide valid JWT token",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 8. RESOURCE NOT FOUND RESPONSE (RESOURCE_NOT_FOUND)
// ============================================================================
// Endpoint: GET /api/v1/customer/carts/550e8400-e29b-41d4-a716-446655440000
// Status: 404 Not Found
// Triggered when: Cart not found
{
  "data": {
    "errorCode": "RESOURCE_NOT_FOUND",
    "message": "Cart not found with identifier: 550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 9. RESOURCE NOT FOUND RESPONSE (Endpoint Not Found)
// ============================================================================
// Endpoint: GET /api/v1/non-existent-endpoint
// Status: 404 Not Found
// Triggered when: NoHandlerFoundException
{
  "data": {
    "errorCode": "SYS_003",
    "message": "Endpoint not found: http://localhost:8080/api/v1/non-existent-endpoint",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 10. ACCESS DENIED RESPONSE (ACCESS_DENIED)
// ============================================================================
// Endpoint: POST /api/v1/organizer/merch
// Status: 403 Forbidden
// Triggered when: User lacks required role (e.g., CUSTOMER trying to access ORGANIZER endpoint)
{
  "data": {
    "errorCode": "ACCESS_DENIED",
    "message": "You do not have permission to access this resource",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// 11. INTERNAL SERVER ERROR RESPONSE (SYS_005)
// ============================================================================
// Endpoint: Any endpoint
// Status: 500 Internal Server Error
// Triggered when: Unhandled exception
{
  "data": {
    "errorCode": "SYS_005",
    "message": "An unexpected error occurred",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}

// ============================================================================
// ERROR CODE REFERENCE
// ============================================================================
/*
  HTTP Status | Error Code              | Description                          | Exception Class
  ------------|-------------------------|--------------------------------------|------------------------------------------
  400         | SYS_001                 | Request validation failed            | MethodArgumentNotValidException
  400         | VALIDATION_ERROR        | Business logic validation failed     | ValidationException
  401         | AUTHENTICATION_FAILED   | Invalid credentials or no token      | AuthenticationException, JwtAuthenticationEntryPoint
  403         | ACCESS_DENIED           | User lacks required role/permission  | ForbiddenException
  404         | RESOURCE_NOT_FOUND      | Entity not found                     | ResourceNotFoundException
  404         | SYS_003                 | Endpoint not found                   | NoHandlerFoundException
  409         | CONFLICT                | Resource already exists              | ConflictException
  500         | SYS_005                 | Unhandled exception                  | Exception (catch-all)
*/

// ============================================================================
// TRACE ID USAGE
// ============================================================================
/*
  Every response includes a unique traceId for distributed tracing.
  
  Usage:
  1. Client receives response with traceId
  2. Client stores traceId for error reporting
  3. Admin can search logs using traceId to find relevant entries
  
  Example:
  - Response traceId: "550e8400-e29b-41d4-a716-446655440000"
  - Log entry: "[traceId=550e8400-e29b-41d4-a716-446655440000] User login successful"
*/

// ============================================================================
// PAGINATION QUERY PARAMETERS
// ============================================================================
/*
  For list endpoints, use query parameters for pagination:
  
  GET /api/v1/public/merch?page=0&pageSize=20&sort=name,asc
  
  Parameters:
  - page: Page number (0-indexed, default: 0)
  - pageSize: Items per page (default: 20, max: 100)
  - sort: Sorting criteria (optional, format: field,asc|desc)
  
  Response meta will include:
  - page: Current page
  - pageSize: Items per page
  - totalElements: Total count
  - totalPages: Total pages
  - hasNext: Whether more pages exist
  - hasPrevious: Whether previous pages exist
*/
