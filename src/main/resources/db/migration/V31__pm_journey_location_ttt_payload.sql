ALTER TABLE pm_journey_locations ADD COLUMN IF NOT EXISTS ttt_payload JSONB;
ALTER TABLE pm_journey_locations_audit ADD COLUMN IF NOT EXISTS ttt_payload JSONB;
