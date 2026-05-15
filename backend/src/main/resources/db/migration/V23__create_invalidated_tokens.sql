-- Persists blacklisted JWT tokens (access + refresh) across restarts.
-- token_hash is SHA-256 of the raw token, stored as 64-char hex.
CREATE TABLE invalidated_tokens (
    token_hash  VARCHAR(64)              NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (token_hash)
);

CREATE INDEX idx_invalidated_tokens_expires_at ON invalidated_tokens (expires_at);
