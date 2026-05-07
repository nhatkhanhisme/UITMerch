-- Add email verification support for users

ALTER TABLE users
    ADD COLUMN is_email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE email_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_email_verifications_user_id ON email_verifications(user_id);
