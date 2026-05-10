-- V8__create_wishlists.sql

-- Chỉ user đã login mới có wishlist
CREATE TABLE wishlists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE wishlist_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id     UUID NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    merch_id        UUID NOT NULL REFERENCES merch_items(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (wishlist_id, merch_id)
);