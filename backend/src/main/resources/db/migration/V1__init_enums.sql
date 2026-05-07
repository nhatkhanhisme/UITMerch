-- V1_01__init_enums.sql
-- Create all database enums for UITMerch
-- These must be created first as they are referenced by table definitions

CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ORGANIZER', 'ADMIN');
CREATE TYPE organization_status AS ENUM ('PENDING', 'ACTIVE', 'INACTIVE');
CREATE TYPE cart_status AS ENUM ('ACTIVE', 'CHECKED_OUT');
CREATE TYPE order_status AS ENUM ('PENDING', 'CONFIRMED', 'READY_FOR_PICKUP', 'SUCCESS', 'CANCELLED');
CREATE TYPE payment_method AS ENUM ('CASH_ON_DELIVERY');