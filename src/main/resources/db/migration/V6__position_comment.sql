-- V6: Add comment field to order positions

ALTER TABLE order_positions ADD COLUMN IF NOT EXISTS comment VARCHAR(2000);
ALTER TABLE order_positions_audit ADD COLUMN IF NOT EXISTS comment VARCHAR(2000);
