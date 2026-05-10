-- V9__create_indexes.sql

-- users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- otp_tokens
CREATE INDEX idx_otp_tokens_user_id ON otp_tokens(user_id);

-- organizations
CREATE INDEX idx_organizations_owner_id ON organizations(owner_id);
CREATE INDEX idx_organizations_status ON organizations(status);

-- merch_items
CREATE INDEX idx_merch_items_org_id ON merch_items(org_id);
CREATE INDEX idx_merch_items_status ON merch_items(status);

-- events
CREATE INDEX idx_events_org_id ON events(org_id);
CREATE INDEX idx_events_status ON events(status);

-- event_merch
CREATE INDEX idx_event_merch_merch_id ON event_merch(merch_id);

-- carts
CREATE INDEX idx_carts_user_id ON carts(user_id);

-- cart_items
CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_merch_id ON cart_items(merch_id);

-- orders
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_org_id ON orders(org_id);
CREATE INDEX idx_orders_status ON orders(status);

-- order_items
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_merch_id ON order_items(merch_id);

-- wishlists
CREATE INDEX idx_wishlists_user_id ON wishlists(user_id);

-- wishlist_items
CREATE INDEX idx_wishlist_items_wishlist_id ON wishlist_items(wishlist_id);
CREATE INDEX idx_wishlist_items_merch_id ON wishlist_items(merch_id);