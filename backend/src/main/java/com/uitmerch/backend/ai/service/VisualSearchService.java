package com.uitmerch.backend.ai.service;

import com.uitmerch.backend.ai.dto.VisualSearchResponse;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.service.MerchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisualSearchService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final VisionAiService visionAiService;
    private final MerchService merchService;

    public VisualSearchResponse search(MultipartFile file) {
        validateFile(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new ValidationException("Không thể đọc file ảnh.");
        }

        String aiDescription = visionAiService.describeImage(bytes, file.getContentType());

        List<MerchResponse> results = searchByKeywords(aiDescription);

        return VisualSearchResponse.builder()
            .aiDescription(aiDescription)
            .results(results)
            .build();
    }

    private List<MerchResponse> searchByKeywords(String aiDescription) {
        for (String keyword : aiDescription.split(",")) {
            String trimmed = keyword.trim();
            if (trimmed.isBlank()) continue;
            List<MerchResponse> results = merchService
                .listPublished(trimmed, null, PageRequest.of(0, 8))
                .getContent();
            if (!results.isEmpty()) return results;
        }
        return List.of();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Vui lòng chọn một ảnh.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ValidationException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("Ảnh không được vượt quá 5 MB.");
        }
    }
}
