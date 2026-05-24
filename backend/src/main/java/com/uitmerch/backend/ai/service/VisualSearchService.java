package com.uitmerch.backend.ai.service;

import com.uitmerch.backend.ai.dto.MerchWithSimilarity;
import com.uitmerch.backend.ai.dto.VisualSearchResponse;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.service.MerchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisualSearchService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_RESULTS = 8;

    private final VisionAiService visionAiService;
    private final EmbeddingService embeddingService;
    private final MerchEmbeddingService merchEmbeddingService;
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

        List<MerchWithSimilarity> results = vectorSearch(aiDescription);
        if (results.isEmpty()) {
            results = keywordFallback(aiDescription);
        }

        return VisualSearchResponse.builder()
            .aiDescription(aiDescription)
            .results(results)
            .build();
    }

    private List<MerchWithSimilarity> vectorSearch(String description) {
        try {
            float[] queryVec = embeddingService.embed(description);
            List<MerchSimilarityEntry> entries = merchEmbeddingService.findNearest(queryVec, MAX_RESULTS);
            if (entries.isEmpty()) return List.of();

            List<UUID> ids = entries.stream().map(MerchSimilarityEntry::id).toList();
            List<MerchResponse> merch = merchService.getPublishedMerchByIds(ids);

            Map<UUID, Integer> simMap = entries.stream().collect(Collectors.toMap(
                MerchSimilarityEntry::id,
                e -> (int) Math.round(e.similarity() * 100)
            ));

            return merch.stream()
                .map(m -> MerchWithSimilarity.builder()
                    .merch(m)
                    .similarity(simMap.getOrDefault(m.getId(), 0))
                    .build())
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // Fallback for dev profile (no pgvector) or when no embeddings exist yet.
    // Only searches by the first keyword (product type) to avoid generic terms matching everything.
    private List<MerchWithSimilarity> keywordFallback(String aiDescription) {
        String firstKeyword = aiDescription.split(",")[0].trim();
        if (firstKeyword.isBlank()) return List.of();
        return merchService
            .listPublished(firstKeyword, null, PageRequest.of(0, MAX_RESULTS))
            .getContent()
            .stream()
            .map(m -> MerchWithSimilarity.builder().merch(m).similarity(0).build())
            .toList();
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
