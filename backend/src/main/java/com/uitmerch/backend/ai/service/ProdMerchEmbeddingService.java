package com.uitmerch.backend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Profile("!(dev | docker)")
@RequiredArgsConstructor
public class ProdMerchEmbeddingService implements MerchEmbeddingService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    @Async
    @Override
    public void storeAsync(UUID merchId, String text) {
        try {
            float[] vec = embeddingService.embed(text);
            store(merchId, vec);
        } catch (Exception e) {
            log.warn("Failed to store embedding for merch {}: {}", merchId, e.getMessage());
        }
    }

    @Override
    public void store(UUID merchId, float[] vector) {
        jdbcTemplate.update(
            """
            INSERT INTO merch_embeddings (merch_id, embedding, updated_at)
            VALUES (?::uuid, ?::vector, now())
            ON CONFLICT (merch_id) DO UPDATE
                SET embedding = EXCLUDED.embedding, updated_at = now()
            """,
            merchId.toString(), toVectorString(vector)
        );
    }

    // Cosine distance threshold: 0 = identical, 1 = unrelated.
    // Items beyond this distance are considered irrelevant and excluded.
    private static final double SIMILARITY_THRESHOLD = 0.35;

    @Override
    public List<MerchSimilarityEntry> findNearest(float[] queryVec, int limit) {
        String vec = toVectorString(queryVec);
        return jdbcTemplate.query(
            "SELECT merch_id, embedding <=> ?::vector AS distance FROM merch_embeddings " +
            "WHERE embedding <=> ?::vector < ? " +
            "ORDER BY distance " +
            "LIMIT ?",
            (rs, i) -> new MerchSimilarityEntry(
                UUID.fromString(rs.getString("merch_id")),
                Math.round((1.0 - rs.getDouble("distance")) * 100.0) / 100.0
            ),
            vec, vec, SIMILARITY_THRESHOLD, limit
        );
    }

    @Override
    public List<UUID> findAllMerchIdsWithEmbedding() {
        return jdbcTemplate.query(
            "SELECT merch_id FROM merch_embeddings",
            (rs, i) -> UUID.fromString(rs.getString("merch_id"))
        );
    }

    private static String toVectorString(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        return sb.append("]").toString();
    }
}
