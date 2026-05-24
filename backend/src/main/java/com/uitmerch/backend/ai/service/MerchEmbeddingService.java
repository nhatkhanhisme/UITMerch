package com.uitmerch.backend.ai.service;

import java.util.List;
import java.util.UUID;

public interface MerchEmbeddingService {
    void storeAsync(UUID merchId, String text);
    void store(UUID merchId, float[] vector);
    List<MerchSimilarityEntry> findNearest(float[] queryVec, int limit);
    List<UUID> findAllMerchIdsWithEmbedding();
}
