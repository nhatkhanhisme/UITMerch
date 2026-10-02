ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN record_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE otp_tokens ADD COLUMN password_reset BOOLEAN NOT NULL DEFAULT FALSE;
-- Old codes have no trustworthy purpose. Require a fresh code after rollout.
UPDATE otp_tokens SET is_used = TRUE;

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_auth_sessions_user ON auth_sessions(user_id);
CREATE INDEX idx_auth_sessions_expiry ON auth_sessions(expires_at);
