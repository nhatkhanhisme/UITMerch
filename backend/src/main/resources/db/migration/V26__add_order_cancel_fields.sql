-- V26: Add cancellation metadata columns to orders table
--      and relax the guest constraint (campus pickup — no shipping address needed)

ALTER TABLE orders
    ADD COLUMN cancelled_by        VARCHAR(20),
    ADD COLUMN cancel_reason       TEXT,
    ADD COLUMN cancel_reason_note  TEXT,
    ADD COLUMN cancelled_at        TIMESTAMP;

-- Drop the old constraint that required guest_address
ALTER TABLE orders DROP CONSTRAINT chk_guest_info;

-- New constraint: guest orders only need name + phone (no address for campus pickup)
ALTER TABLE orders ADD CONSTRAINT chk_guest_info CHECK (
    user_id IS NOT NULL OR (
        guest_name  IS NOT NULL AND
        guest_phone IS NOT NULL
    )
);
