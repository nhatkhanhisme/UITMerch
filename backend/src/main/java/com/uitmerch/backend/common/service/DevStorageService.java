package com.uitmerch.backend.common.service;

import com.uitmerch.backend.common.exception.StorageException;
import com.uitmerch.backend.common.util.FileUploadResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Profile("dev")
public class DevStorageService implements StorageService {

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String bucket) {
        return uploadFile(file, bucket, "dev");
    }

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String bucket, String folderPath) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty or null");
        }

        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : UUID.randomUUID().toString();
        String fileUrl = "/dev-storage/" + bucket + "/" + folderPath + "/" + originalFileName;

        return FileUploadResponse.builder()
                .fileName(originalFileName)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();
    }

    @Override
    public void deleteFile(String bucket, String filePath) {
        // no-op in dev
    }
}
