-- V34__add_otp_purpose.sql
-- Adds purpose column to otp_tokens so VERIFY_EMAIL and RESET_PASSWORD OTPs
-- cannot be used interchangeably. Existing rows default to VERIFY_EMAIL.
ALTER TABLE otp_tokens
    ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'VERIFY_EMAIL';
