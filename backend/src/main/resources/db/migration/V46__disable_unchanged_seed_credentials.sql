-- Preserve historical migrations/checksums and all business data.
-- Only accounts still carrying the exact public V12/V14 seed hash are disabled.
-- Accounts whose owners already changed their password are unaffected.
WITH disabled AS (
    UPDATE users
    SET is_active = FALSE, auth_version = auth_version + 1,
        record_version = record_version + 1, updated_at = NOW()
    WHERE password_hash = '$2a$10$bJMeGHBL0q8J4kceezsn7uR48Kcm45xd2UpNwzJzJkJCh2e7hJPqe'
    RETURNING id
)
UPDATE auth_sessions SET revoked_at = NOW()
WHERE revoked_at IS NULL AND user_id IN (SELECT id FROM disabled);

UPDATE otp_tokens SET is_used = TRUE
WHERE user_id IN (
    SELECT id FROM users
    WHERE password_hash = '$2a$10$bJMeGHBL0q8J4kceezsn7uR48Kcm45xd2UpNwzJzJkJCh2e7hJPqe'
);
