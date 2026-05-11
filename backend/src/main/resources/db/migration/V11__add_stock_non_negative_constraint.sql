ALTER TABLE merch_items ADD CONSTRAINT chk_stock_non_negative CHECK (stock >= 0);
