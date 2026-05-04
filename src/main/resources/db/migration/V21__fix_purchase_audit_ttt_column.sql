-- V21: Add ttt_order_attributes to purchase_positions_audit
--
-- V17 was modified after initial execution (checksum repaired), so the
-- ALTER TABLE for purchase_positions_audit never ran against the database.

ALTER TABLE purchase_positions_audit
    ADD COLUMN IF NOT EXISTS ttt_order_attributes JSONB;
