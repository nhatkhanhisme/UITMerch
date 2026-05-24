package com.uitmerch.backend.ai.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Profile("dev | docker")
public class DevMerchEmbeddingService implements MerchEmbeddingService {

    @Override public void storeAsync(UUID merchId, String text) {}
    @Override public void store(UUID merchId, float[] vector) {}
    @Override public List<MerchSimilarityEntry> findNearest(float[] queryVec, int limit) { return List.of(); }
    @Override public List<UUID> findAllMerchIdsWithEmbedding() { return List.of(); }
}
