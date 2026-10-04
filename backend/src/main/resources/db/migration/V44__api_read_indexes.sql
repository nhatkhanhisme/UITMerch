-- Concurrent builds preserve writes on existing installations.
-- This migration runs outside a transaction (see the companion .conf).
CREATE INDEX CONCURRENTLY idx_orders_user_created_id ON orders(user_id, created_at DESC, id DESC);
CREATE INDEX CONCURRENTLY idx_orders_org_pickup_created_id ON orders(org_id, pickup_schedule_id, created_at DESC, id DESC);
CREATE INDEX CONCURRENTLY idx_notifications_user_created_id ON notifications(user_id, created_at DESC, id DESC);
