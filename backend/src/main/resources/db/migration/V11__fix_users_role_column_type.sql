-- Fix role column type to match Hibernate @Enumerated(EnumType.STRING)
-- PostgreSQL enum type doesn't work well with Hibernate string-based enum storage
ALTER TABLE users
    ALTER COLUMN role DROP DEFAULT,
    ALTER COLUMN role TYPE VARCHAR(50);

-- Re-add default after type change
ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'CUSTOMER';
