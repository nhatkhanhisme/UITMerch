-- V10__fix_users_full_name_not_null.sql
-- Align full_name with entity @Column(nullable = false); safe because the
-- registration DTO enforces @NotBlank so no null rows exist.
ALTER TABLE users ALTER COLUMN full_name SET NOT NULL;
