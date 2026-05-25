-- V31: Add NEW_ORDER value to notification_type enum (for organizer notifications)
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'NEW_ORDER';
