-- V45: Per-demand Verkehrstage + von/nach on resource needs (A-S1). A demand can now apply to its own
-- day-set within the position (e.g. weekend train attendants while the path stays the same) and carry
-- a from/to for FAHRPLAN positions. validity mirrors order_positions.validity (JSONB date-set).

ALTER TABLE resource_needs ADD COLUMN validity JSONB;
ALTER TABLE resource_needs ADD COLUMN from_location VARCHAR(255);
ALTER TABLE resource_needs ADD COLUMN to_location VARCHAR(255);

ALTER TABLE resource_needs_audit ADD COLUMN validity JSONB;
ALTER TABLE resource_needs_audit ADD COLUMN from_location VARCHAR(255);
ALTER TABLE resource_needs_audit ADD COLUMN to_location VARCHAR(255);
