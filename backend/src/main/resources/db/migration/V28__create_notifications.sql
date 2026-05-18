-- V28: In-app notifications for customers

CREATE TYPE notification_type AS ENUM (
    'ORDER_CONFIRMED',
    'ORDER_READY',
    'ORDER_COMPLETED',
    'ORDER_CANCELLED',
    'PICKUP_SCHEDULED'
);

CREATE TABLE notifications (
    id               UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    message          TEXT    NOT NULL,
    type             notification_type NOT NULL,
    is_read          BOOLEAN NOT NULL DEFAULT FALSE,
    related_order_id UUID    REFERENCES orders(id) ON DELETE SET NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id     ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
