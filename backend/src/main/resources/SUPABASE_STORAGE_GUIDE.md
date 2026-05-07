## Supabase Storage Service Documentation

### Overview

UITMerch uses Supabase Storage (S3-compatible) for all file uploads instead of storing files as BLOB in the database.

**NFR03: Supabase Storage used for images; Base64/BLOB persistence in DB is strictly prohibited.**

This service:
- Handles MultipartFile uploads to Supabase Storage
- Generates unique file names (UUID-based to avoid conflicts)
- Returns public URLs for uploaded files
- Validates file type (images only) and size (max 10MB)
- Can delete files from storage when needed

---

### Architecture

```
Controller (receives MultipartFile)
    ↓
StorageService.uploadFile()
    ↓
SupabaseStorageServiceImpl
    ↓
AWS S3 SDK (with custom Supabase endpoint)
    ↓
Supabase Storage (S3-compatible)
    ↓
Returns: Public URL string
    ↓
Database stores ONLY the URL, NOT the file content
```

---

### Configuration

#### Environment Variables (Required)

```bash
# Supabase Storage credentials - get from Supabase dashboard
export APP_STORAGE_ENDPOINT="https://YOUR_PROJECT.supabase.co/storage/v1/s3"
export APP_STORAGE_REGION="us-east-1"
export APP_STORAGE_ACCESS_KEY="YOUR_SERVICE_ROLE_KEY"
export APP_STORAGE_SECRET_KEY="YOUR_SECRET_KEY"
export APP_STORAGE_PUBLIC_URL="https://YOUR_PROJECT.supabase.co/storage/v1/object/public"
```

#### .env File (Local Development)

```
APP_STORAGE_ENDPOINT=https://YOUR_PROJECT.supabase.co/storage/v1/s3
APP_STORAGE_REGION=us-east-1
APP_STORAGE_ACCESS_KEY=YOUR_SERVICE_ROLE_KEY
APP_STORAGE_SECRET_KEY=YOUR_SECRET_KEY
APP_STORAGE_PUBLIC_URL=https://YOUR_PROJECT.supabase.co/storage/v1/object/public
```

#### application.yaml

```yaml
app:
  storage:
    endpoint: ${APP_STORAGE_ENDPOINT:https://YOUR_PROJECT.supabase.co/storage/v1/s3}
    region: ${APP_STORAGE_REGION:us-east-1}
    access-key: ${APP_STORAGE_ACCESS_KEY:}
    secret-key: ${APP_STORAGE_SECRET_KEY:}
    public-url: ${APP_STORAGE_PUBLIC_URL:https://YOUR_PROJECT.supabase.co/storage/v1/object/public}
```

---

### Usage in Controllers

#### Upload Merch Image

```java
@RestController
@RequestMapping("/api/v1/organizer/merch")
public class MerchController {
    
    private final StorageService storageService;
    
    public MerchController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<MerchResponse>> createMerch(
        @Valid @RequestBody CreateMerchRequest request,
        @RequestParam("image") MultipartFile imageFile
    ) {
        // Upload image to Supabase Storage
        FileUploadResponse uploadResponse = storageService.uploadFile(
            imageFile, 
            "merch-images"  // bucket name
        );
        
        // Use returned URL to save in database
        MerchResponse response = merchService.createMerch(
            request,
            uploadResponse.getFileUrl()  // Store only the URL
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }
}
```

#### Upload Organization Logo

```java
@RestController
@RequestMapping("/api/v1/organizer/profile")
public class OrganizationController {
    
    private final StorageService storageService;
    
    public OrganizationController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    @PatchMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
        @Valid @RequestBody UpdateOrgRequest request,
        @RequestParam("logo") MultipartFile logoFile
    ) {
        // Upload logo with custom folder path
        FileUploadResponse uploadResponse = storageService.uploadFile(
            logoFile,
            "org-logos",
            "2026/05"  // optional folder path
        );
        
        // Update organization with logo URL
        OrganizationResponse response = orgService.updateOrganization(
            request,
            uploadResponse.getFileUrl()
        );
        
        return ResponseEntity.ok(
            ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId())
        );
    }
}
```

---

### API Methods

#### 1. Upload File (Default Folder)

```java
FileUploadResponse uploadFile(MultipartFile file, String bucket)
```

**Example:**
```java
FileUploadResponse response = storageService.uploadFile(
    imageFile,
    "merch-images"
);
// Response:
// {
//   fileName: "hoodie.jpg",
//   fileUrl: "https://storage.supabase.co/storage/v1/object/public/merch-images/2026-05-06/550e8400-e29b-41d4-a716-446655440000.jpg",
//   fileSize: 1048576,
//   mimeType: "image/jpeg"
// }
```

#### 2. Upload File (Custom Folder)

```java
FileUploadResponse uploadFile(MultipartFile file, String bucket, String folderPath)
```

**Example:**
```java
FileUploadResponse response = storageService.uploadFile(
    imageFile,
    "org-logos",
    "2026/05"  // custom folder
);
```

#### 3. Delete File

```java
void deleteFile(String bucket, String filePath)
```

**Example:**
```java
storageService.deleteFile(
    "merch-images",
    "2026-05-06/550e8400-e29b-41d4-a716-446655440000.jpg"
);
```

---

### Bucket Structure

Create these buckets in Supabase:

1. **merch-images** - For product/merchandise images
   - Folder structure: `YYYY-MM-DD/UUID.ext`
   - Example: `2026-05-06/550e8400-e29b-41d4-a716-446655440000.jpg`

2. **org-logos** - For organization/club logos
   - Folder structure: `YYYY/MM/UUID.ext`
   - Example: `2026/05/550e8400-e29b-41d4-a716-446655440001.png`

---

### File Validation

**Allowed MIME Types:**
- image/jpeg
- image/png
- image/gif
- image/webp
- image/svg+xml

**Maximum File Size:** 10MB

**File Naming:** Automatic UUID generation to prevent conflicts

---

### Error Handling

File upload errors throw `StorageException` which is automatically caught by `GlobalExceptionHandler`:

**Error Codes:**
- `STORAGE_ERROR` - HTTP 500 (internal server error)

**Error Response:**
```json
{
  "data": {
    "errorCode": "STORAGE_ERROR",
    "message": "File size exceeds maximum allowed size of 10MB",
    "timestamp": "2026-05-06T10:30:00.000Z"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Possible Errors:**
- File is empty or null
- File size exceeds 10MB
- Unsupported file type (not an image)
- S3 upload failure
- Network connectivity issues

---

### Database Storage

**DO NOT store files as BLOB:**

❌ WRONG:
```java
@Column(columnDefinition = "BYTEA")
private byte[] imageContent;  // NO! This violates NFR03
```

✅ CORRECT:
```java
@Column(columnDefinition = "TEXT")
private String imageUrl;  // Store only the public URL
```

Entity examples:

```java
@Entity
@Table(name = "merch_items")
public class MerchItem {
    // ...
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;  // ✅ Correct: URL only
    
    // NOT:
    // @Column(columnDefinition = "BYTEA")
    // private byte[] imageContent;  // ❌ Wrong: BLOB storage
}

@Entity
@Table(name = "organizations")
public class Organization {
    // ...
    
    @Column(columnDefinition = "TEXT")
    private String logoUrl;  // ✅ Correct: URL only
}
```

---

### Supabase Setup

#### Step 1: Create Project

1. Go to [supabase.com](https://supabase.com)
2. Create new project
3. Get credentials from dashboard

#### Step 2: Create Storage Buckets

1. Go to Storage in Supabase dashboard
2. Create new bucket: **merch-images**
3. Create new bucket: **org-logos**
4. Set policies for public read access

#### Step 3: Get Credentials

In Supabase Project Settings → API:
- Copy **Service Role Key** → `APP_STORAGE_ACCESS_KEY`
- Copy **Anonymous Key** → Can be used for public URLs
- Project URL → Extract endpoint from project URL

#### Step 4: Configure Environment

Set environment variables with credentials from Step 3

---

### Example: Complete Merch Upload Flow

#### 1. Frontend (sends file)
```javascript
const formData = new FormData();
formData.append("image", imageFile);
formData.append("request", JSON.stringify({
  name: "UITMerch Hoodie",
  meaningText: "...",
  price: 150000,
  // ...
}));

fetch("/api/v1/organizer/merch", {
  method: "POST",
  headers: {
    "Authorization": "Bearer " + accessToken
  },
  body: formData
});
```

#### 2. Backend Controller
```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<MerchResponse>> createMerch(
    @ModelAttribute @Valid CreateMerchRequest request,
    @RequestParam("image") MultipartFile imageFile
) {
    // Upload image to Supabase
    FileUploadResponse uploadResponse = storageService.uploadFile(
        imageFile,
        "merch-images"
    );
    
    // Create merch with URL
    MerchResponse response = merchService.createMerch(
        request,
        uploadResponse.getFileUrl()  // Only store URL
    );
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, traceId));
}
```

#### 3. Response
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "UITMerch Hoodie",
    "imageUrl": "https://storage.supabase.co/storage/v1/object/public/merch-images/2026-05-06/550e8400-e29b-41d4-a716-446655440001.jpg",
    "price": 150000,
    "// ...": ""
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Testing

#### Local Testing without Supabase

Create a mock `StorageService`:

```java
@Component
@Profile("test")
public class MockStorageService implements StorageService {
    
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String bucket) {
        return FileUploadResponse.builder()
            .fileName(file.getOriginalFilename())
            .fileUrl("https://example.com/mock-url.jpg")
            .fileSize(file.getSize())
            .mimeType(file.getContentType())
            .build();
    }
    
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String bucket, String folderPath) {
        return uploadFile(file, bucket);
    }
    
    @Override
    public void deleteFile(String bucket, String filePath) {
        // Mock: do nothing
    }
}
```

---

### Troubleshooting

| Issue | Solution |
|-------|----------|
| "Invalid AWS credentials" | Check APP_STORAGE_ACCESS_KEY and APP_STORAGE_SECRET_KEY |
| "Bucket not found" | Ensure bucket names exist in Supabase Storage |
| "File type not allowed" | Only images allowed (JPEG, PNG, GIF, WebP, SVG) |
| "File size exceeds limit" | Maximum 10MB per file |
| "Connection timeout" | Check APP_STORAGE_ENDPOINT URL and network connectivity |

---

### Performance Considerations

- **Async Upload**: For large files, consider async upload in future versions
- **CDN**: Supabase automatically serves files through CDN
- **Concurrent Uploads**: AWS SDK handles connection pooling
- **File Cleanup**: Implement scheduled task to clean orphaned files

---

### Security Considerations

✅ **Do:**
- Keep APP_STORAGE_SECRET_KEY secure (never expose in logs/frontend)
- Use Service Role Key for backend uploads
- Validate file type and size server-side
- Set appropriate Supabase policies for public/private access

❌ **Don't:**
- Store credentials in code (use environment variables)
- Store file content in database (BLOB)
- Allow arbitrary file types
- Allow unlimited file sizes
- Expose secret keys in API responses

---

### Future Enhancements

1. **Async Upload**: Use @Async for non-blocking uploads
2. **Image Optimization**: Resize/compress images before upload
3. **CDN Integration**: Leverage Supabase CDN for caching
4. **Multi-region**: Support uploads to different storage regions
5. **Batch Upload**: Handle multiple file uploads in one request
6. **Cleanup Task**: Scheduled deletion of orphaned files
