CREATE INDEX idx_orders_org_created ON orders(org_id, created_at);
CREATE INDEX idx_pickup_schedules_org_date ON pickup_schedules(org_id, pickup_date);
