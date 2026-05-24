CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE merch_embeddings (
    merch_id   UUID PRIMARY KEY REFERENCES merch_items(id) ON DELETE CASCADE,
    embedding  vector(768),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX merch_embeddings_hnsw_idx
    ON merch_embeddings USING hnsw (embedding vector_cosine_ops);
