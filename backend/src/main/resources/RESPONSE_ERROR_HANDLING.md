## ✅ Response & Error Handling Architecture - Complete

### **What We've Built**

A complete, production-ready response and error handling system that:
- Wraps ALL responses in a standardized envelope
- Handles ALL exception types globally
- Provides detailed validation error feedback
- Includes distributed tracing via traceId
- Supports pagination metadata for list endpoints
- Is fully documented with Swagger/OpenAPI

---

### **Core Components**

#### 1. **ApiResponse<T> - Universal Response Envelope**

```java
{
  "data": T,           // Payload (data or error detail)
  "meta": { ... },     // Optional pagination metadata
  "traceId": "uuid"    // Unique request ID for tracing
}
```

**Helper Methods:**
- `ApiResponse.success(data, traceId)` → Single object response
- `ApiResponse.successWithMeta(data, meta, traceId)` → Paginated list response

#### 2. **GlobalExceptionHandler - Exception Mapping**

| Exception | Error Code | HTTP Status | Use Case |
|-----------|-----------|------------|----------|
| MethodArgumentNotValidException | SYS_001 | 400 | @Valid annotation failures |
| NoHandlerFoundException | SYS_003 | 404 | Endpoint not found |
| ConflictException | CONFLICT | 409 | Duplicate email, resource exists |
| AuthenticationException | AUTHENTICATION_FAILED | 401 | Invalid credentials |
| ForbiddenException | ACCESS_DENIED | 403 | Role/permission check fails |
| ResourceNotFoundException | RESOURCE_NOT_FOUND | 404 | Entity not found |
| ValidationException | VALIDATION_ERROR | 400 | Business logic validation |
| Generic Exception | SYS_005 | 500 | Unhandled errors |

#### 3. **ErrorDetail - Error Response Structure**

```java
{
  "errorCode": "SYS_001",
  "message": "Request validation failed",
  "timestamp": "2026-05-06T10:30:00.000Z",
  "fieldErrors": [  // Only for SYS_001
    {
      "field": "email",
      "message": "must be a valid email address",
      "rejectedValue": "invalid"
    }
  ]
}
```

#### 4. **PaginationMeta - Pagination Support**

```java
{
  "page": 0,
  "pageSize": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

---

### **Exception Hierarchy**

```
Throwable
├── Exception
│   └── RuntimeException
│       └── AppException (base domain exception)
│           ├── ValidationException (400)
│           ├── ConflictException (409)
│           ├── AuthenticationException (401)
│           ├── ForbiddenException (403)
│           └── ResourceNotFoundException (404)
│
└── Spring Exceptions
    ├── MethodArgumentNotValidException → SYS_001
    ├── NoHandlerFoundException → SYS_003
    └── Generic → SYS_005
```

---

### **Request/Response Examples**

#### Example 1: Successful Single Object (POST)
**Request:**
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response: 200 OK**
```json
{
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "CUSTOMER",
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "expiresIn": 86400000
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

#### Example 2: Paginated List (GET)
**Request:**
```
GET /api/v1/public/merch?page=0&pageSize=20
```

**Response: 200 OK**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "UITMerch Hoodie",
      "price": 150000.00,
      ...
    },
    ...
  ],
  "meta": {
    "page": 0,
    "pageSize": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

#### Example 3: Validation Error (SYS_001)
**Request:**
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "short",
  "fullName": ""
}
```

**Response: 400 Bad Request**
```json
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
        "message": "size must be between 8 and 255",
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
```

---

#### Example 4: Business Logic Validation (VALIDATION_ERROR)
**Request:**
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "ValidPass123",
  "fullName": "John Doe"
}
```

**Response: 400 Bad Request** (email already exists)
```json
{
  "data": {
    "errorCode": "CONFLICT",
    "message": "Email already exists: user@example.com",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

#### Example 5: JWT Authentication Failure
**Request:**
```
GET /api/v1/customer/carts
Authorization: Bearer invalid-or-expired-token
```

**Response: 401 Unauthorized**
```json
{
  "data": {
    "errorCode": "AUTHENTICATION_FAILED",
    "message": "Unauthorized access - please provide valid JWT token",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

#### Example 6: Resource Not Found
**Request:**
```
GET /api/v1/customer/carts/550e8400-e29b-41d4-a716-invalid
```

**Response: 404 Not Found**
```json
{
  "data": {
    "errorCode": "RESOURCE_NOT_FOUND",
    "message": "Cart not found with identifier: 550e8400-e29b-41d4-a716-invalid",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

#### Example 7: Access Denied (Insufficient Role)
**Request:**
```
POST /api/v1/organizer/merch
Authorization: Bearer customer-jwt-token
Content-Type: application/json

{ ... }
```

**Response: 403 Forbidden**
```json
{
  "data": {
    "errorCode": "ACCESS_DENIED",
    "message": "You do not have permission to access this resource",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### **Key Features**

✅ **Unified Response Format** - All endpoints follow same envelope structure  
✅ **Automatic Error Catching** - @RestControllerAdvice catches ALL exceptions  
✅ **Detailed Validation Feedback** - Field-level error details for SYS_001  
✅ **Distributed Tracing** - TraceId in every response for log correlation  
✅ **Standardized Error Codes** - Consistent error codes across all modules  
✅ **JSON Null Exclusion** - Uses @JsonInclude(NON_NULL) to keep responses clean  
✅ **Swagger Documentation** - @Schema annotations for OpenAPI generation  
✅ **Pagination Support** - Built-in pagination metadata for list endpoints  
✅ **No Field Injection** - Constructor-based dependency injection ready  

---

### **Implementation Checklist**

- ✅ ApiResponse<T> generic wrapper (data, meta, traceId)
- ✅ PaginationMeta for list responses
- ✅ GlobalExceptionHandler with @RestControllerAdvice
- ✅ MethodArgumentNotValidException handler (SYS_001 with fieldErrors)
- ✅ NoHandlerFoundException handler (SYS_003)
- ✅ Generic exception fallback handler (SYS_005)
- ✅ ConflictException (409)
- ✅ AuthenticationException (401)
- ✅ ForbiddenException (403)
- ✅ ResourceNotFoundException (404)
- ✅ ValidationException (400)
- ✅ Swagger @Schema annotations
- ✅ Comprehensive API_RESPONSE_FORMATS.md documentation

---

### **How to Use in Controllers**

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final String traceId = TraceIdUtil.getOrCreateTraceId();
    
    // Success responses
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(
            ApiResponse.success(tokenResponse, traceId)
        );
    }
    
    // Throws ConflictException automatically caught
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        // If email exists → throws ConflictException → 409 CONFLICT
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, traceId));
    }
    
    // Paginated list - uses meta
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        Page<UserDto> users = authService.getUsers(page, pageSize);
        PaginationMeta meta = PaginationMeta.builder()
            .page(users.getNumber())
            .pageSize(users.getSize())
            .totalElements(users.getTotalElements())
            .totalPages(users.getTotalPages())
            .hasNext(users.hasNext())
            .hasPrevious(users.hasPrevious())
            .build();
        
        return ResponseEntity.ok(
            ApiResponse.successWithMeta(users.getContent(), meta, traceId)
        );
    }
}
```

---

### **Key Design Principles**

1. **Envelope Everything** - No raw responses, all wrapped in ApiResponse
2. **Central Error Handling** - GlobalExceptionHandler catches everything
3. **Clear Error Codes** - SYS_XXX for system errors, domain-specific for business errors
4. **Detailed Feedback** - Validation errors include field-level details
5. **Traceability** - Every response includes traceId for log correlation
6. **Clean JSON** - @JsonInclude(NON_NULL) prevents unnecessary null fields
7. **Consistency** - Same format regardless of endpoint or error type

---

### **Files Created/Modified**

- ✅ `ApiResponse<T>` - Enhanced with Swagger docs and helpers
- ✅ `PaginationMeta` - Enhanced with Swagger docs
- ✅ `GlobalExceptionHandler` - Complete rewrite with 4 handlers
- ✅ `ConflictException` - New domain exception
- ✅ `AuthenticationException` - New domain exception
- ✅ `ForbiddenException` - New domain exception
- ✅ `API_RESPONSE_FORMATS.md` - Comprehensive documentation

---

**Ready to implement the Auth Service & Controller to complete the authentication module?**
