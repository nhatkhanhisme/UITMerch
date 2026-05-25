-- V30: Add ORDER_PLACED value to notification_type enum
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'ORDER_PLACED';
