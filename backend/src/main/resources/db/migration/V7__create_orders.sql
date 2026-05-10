-- V7__create_orders.sql

CREATE TABLE orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Nullable: null nếu là guest order
    user_id             UUID NULL REFERENCES users(id) ON DELETE SET NULL,

    -- Org nhận đơn
    org_id              UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,

    -- Guest info: bắt buộc nếu user_id IS NULL
    guest_name          VARCHAR(255),
    guest_email         VARCHAR(255),
    guest_phone         VARCHAR(20),
    guest_address       TEXT,

    -- Snapshot tổng tiền tại thời điểm đặt
    total_amount        NUMERIC(12, 2) NOT NULL,

    status              order_status NOT NULL DEFAULT 'PENDING',
    payment_method      payment_method NOT NULL DEFAULT 'CASH_ON_DELIVERY',
    payment_status      payment_status NOT NULL DEFAULT 'PENDING',

    note                TEXT,

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Đảm bảo guest phải có thông tin
    CONSTRAINT chk_guest_info CHECK (
        user_id IS NOT NULL OR (
            guest_name IS NOT NULL AND
            guest_phone IS NOT NULL AND
            guest_address IS NOT NULL
        )
    )
);

CREATE TABLE order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    merch_id        UUID NOT NULL REFERENCES merch_items(id) ON DELETE RESTRICT,

    -- Snapshot tên và giá tại thời điểm đặt
    merch_name      VARCHAR(255) NOT NULL,
    unit_price      NUMERIC(12, 2) NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    subtotal        NUMERIC(12, 2) NOT NULL,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);