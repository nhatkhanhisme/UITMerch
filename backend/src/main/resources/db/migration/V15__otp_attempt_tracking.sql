-- V15__otp_attempt_tracking.sql
-- Adds attempt counting and lock-out to otp_tokens to prevent brute-force.

ALTER TABLE otp_tokens
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until  TIMESTAMP;
