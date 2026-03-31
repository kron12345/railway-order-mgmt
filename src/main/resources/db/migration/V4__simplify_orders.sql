-- V4: Simplify orders — remove timetableYearLabel, make validFrom/validTo NOT NULL

-- Set defaults for existing rows
UPDATE orders SET valid_from = CURRENT_DATE WHERE valid_from IS NULL;
UPDATE orders SET valid_to = CURRENT_DATE + INTERVAL '1 year' WHERE valid_to IS NULL;

ALTER TABLE orders ALTER COLUMN valid_from SET NOT NULL;
ALTER TABLE orders ALTER COLUMN valid_to SET NOT NULL;
ALTER TABLE orders DROP COLUMN IF EXISTS timetable_year_label;

-- Update audit table
ALTER TABLE orders_audit DROP COLUMN IF EXISTS timetable_year_label;
