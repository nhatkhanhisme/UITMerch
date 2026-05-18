-- V25: Rename order status enum values to align with campus-pickup flow
-- READY_FOR_PICKUP → READY  (simpler label now that there's no shipping)
-- SUCCESS           → COMPLETED (clearer terminal state name)

ALTER TYPE order_status RENAME VALUE 'READY_FOR_PICKUP' TO 'READY';
ALTER TYPE order_status RENAME VALUE 'SUCCESS' TO 'COMPLETED';
