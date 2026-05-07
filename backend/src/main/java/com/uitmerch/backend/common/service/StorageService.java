package com.uitmerch.backend.common.service;

import com.uitmerch.backend.common.util.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage service interface for file uploads.
 * NFR03: Supabase Storage used for images; Base64/BLOB persistence in DB is strictly prohibited.
 */
public interface StorageService {
    
    /**
     * Upload a file to Supabase Storage.
     * @param file MultipartFile to upload (image only)
     * @param bucket bucket name (e.g., "merch-images", "org-logos")
     * @return FileUploadResponse with public URL
     * @throws StorageException if upload fails
     */
    FileUploadResponse uploadFile(MultipartFile file, String bucket);
    
    /**
     * Upload a file with custom folder path.
     * @param file MultipartFile to upload
     * @param bucket bucket name
     * @param folderPath folder path within bucket (e.g., "2026/05")
     * @return FileUploadResponse with public URL
     * @throws StorageException if upload fails
     */
    FileUploadResponse uploadFile(MultipartFile file, String bucket, String folderPath);
    
    /**
     * Delete a file from Supabase Storage.
     * @param bucket bucket name
     * @param filePath file path within bucket
     * @throws StorageException if deletion fails
     */
    void deleteFile(String bucket, String filePath);
}
