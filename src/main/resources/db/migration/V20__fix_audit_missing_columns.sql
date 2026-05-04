-- V20: Fix missing columns in audit tables
--
-- V8 and V9 added columns to *_aud tables instead of *_audit tables.
-- The Envers config uses _audit suffix, so the _audit tables are missing
-- operational_train_number and pm_reference_train_id.
-- Also drop orphaned _aud tables (PM _aud tables were already renamed in V19).

-- ============================================================
-- 1. Add missing columns to order_positions_audit
-- ============================================================
ALTER TABLE order_positions_audit
    ADD COLUMN IF NOT EXISTS operational_train_number VARCHAR(20);

ALTER TABLE order_positions_audit
    ADD COLUMN IF NOT EXISTS pm_reference_train_id UUID;

-- ============================================================
-- 2. Add missing column to timetable_archives_audit
-- ============================================================
ALTER TABLE timetable_archives_audit
    ADD COLUMN IF NOT EXISTS operational_train_number VARCHAR(20);

-- ============================================================
-- 3. Drop orphaned _aud tables (Envers uses _audit suffix)
-- ============================================================
DROP TABLE IF EXISTS order_positions_aud;
DROP TABLE IF EXISTS timetable_archives_aud;
