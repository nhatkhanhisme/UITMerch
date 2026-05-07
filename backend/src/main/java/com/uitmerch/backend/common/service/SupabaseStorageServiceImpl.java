package com.uitmerch.backend.common.service;

import com.uitmerch.backend.common.exception.StorageException;
import com.uitmerch.backend.common.util.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementation of StorageService for Supabase Storage uploads.
 * 
 * Features:
 * - Upload MultipartFile to Supabase Storage
 * - Generate unique file names (UUID-based)
 * - Return public URLs for uploaded files
 * - Handle file deletion
 * 
 * NFR03: Supabase Storage used for images; Base64/BLOB persistence in DB is strictly prohibited.
 * 
 * USAGE:
 * storageService.uploadFile(file, "merch-images")
 * → Returns public URL like: https://storage.supabase.co/...
 * 
 * NO BLOB STORAGE IN DATABASE:
 * Database stores only the fileUrl returned here, not the file content.
 */
@Service
public class SupabaseStorageServiceImpl implements StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorageServiceImpl.class);
    
    // Allowed MIME types for image upload
    private static final String[] ALLOWED_MIME_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "image/svg+xml"
    };
    
    // Maximum file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    @Value("${app.storage.public-url:https://your-project.supabase.co/storage/v1/object/public}")
    private String publicUrlBase;
    
    private final S3Client s3Client;
    
    public SupabaseStorageServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }
    
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String bucket) {
        return uploadFile(file, bucket, LocalDate.now().toString());
    }
    
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String bucket, String folderPath) {
        // Validate file
        validateFile(file);
        
        // Generate unique file name
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = generateUniqueFileName(originalFileName);
        String filePath = folderPath + "/" + uniqueFileName;
        
        try {
            // Upload to Supabase Storage
            uploadToS3(file, bucket, filePath);
            
            // Build public URL
            String publicUrl = buildPublicUrl(bucket, filePath);
            
            logger.info("File uploaded successfully: {} to bucket: {} at path: {}", 
                originalFileName, bucket, filePath);
            
            return FileUploadResponse.builder()
                .fileName(originalFileName)
                .fileUrl(publicUrl)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();
                
        } catch (IOException e) {
            logger.error("Failed to upload file: {}", originalFileName, e);
            throw new StorageException("Failed to upload file: " + originalFileName, e);
        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", originalFileName, e);
            throw new StorageException("Unexpected error during file upload", e);
        }
    }
    
    @Override
    public void deleteFile(String bucket, String filePath) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(filePath)
                .build();
            
            s3Client.deleteObject(deleteRequest);
            logger.info("File deleted successfully from bucket: {} at path: {}", bucket, filePath);
            
        } catch (Exception e) {
            logger.error("Failed to delete file from bucket: {} at path: {}", bucket, filePath, e);
            throw new StorageException("Failed to delete file: " + filePath, e);
        }
    }
    
    /**
     * Upload file to S3 (Supabase Storage).
     */
    private void uploadToS3(MultipartFile file, String bucket, String filePath) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(filePath)
            .contentType(file.getContentType())
            .build();
        
        PutObjectResponse response = s3Client.putObject(
            putObjectRequest,
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        
        if (response == null) {
            throw new StorageException("Failed to upload file to S3: null response");
        }
    }
    
    /**
     * Validate file before upload.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty or null");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageException("File size exceeds maximum allowed size of 10MB");
        }
        
        // Check MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || !isAllowedMimeType(mimeType)) {
            throw new StorageException("File type not allowed. Only images are allowed (JPEG, PNG, GIF, WebP, SVG)");
        }
    }
    
    /**
     * Check if MIME type is allowed.
     */
    private boolean isAllowedMimeType(String mimeType) {
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (mimeType.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Generate unique file name using UUID to avoid conflicts.
     * Preserves original extension.
     */
    private String generateUniqueFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        // Extract extension
        int lastDotIndex = originalFileName.lastIndexOf('.');
        String extension = lastDotIndex > 0 ? originalFileName.substring(lastDotIndex) : "";
        
        // Generate UUID-based name with original extension
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * Build public URL for the uploaded file.
     */
    private String buildPublicUrl(String bucket, String filePath) {
        return publicUrlBase + "/" + bucket + "/" + filePath;
    }
}
