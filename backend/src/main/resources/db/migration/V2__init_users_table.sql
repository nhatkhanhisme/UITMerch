-- V1_02__init_users_table.sql
-- Create users table with role-based access control
-- BR01: email is globally unique and valid format (validation in app layer)
-- BR02: Password >= 8 chars with uppercase, lowercase, number (validation in app layer)

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT email_not_empty CHECK (LENGTH(TRIM(email)) > 0),
    CONSTRAINT password_hash_not_empty CHECK (LENGTH(TRIM(password_hash)) > 0),
    CONSTRAINT full_name_not_empty CHECK (LENGTH(TRIM(full_name)) > 0)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
