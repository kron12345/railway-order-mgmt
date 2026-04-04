-- V19: Fix audit table naming and Envers column names
--
-- The Envers config (audit_table_suffix=_audit, revision_field_name=revision_id,
-- revision_type_field_name=revision_type) was not being applied due to incorrect
-- YAML nesting. V9 created PM audit tables with _aud suffix and V10 renamed columns
-- to rev/revtype. Now that the config is fixed, we need to:
--   1. Rename PM _aud tables to _audit (matching Envers config)
--   2. Rename rev/revtype back to revision_id/revision_type in PM audit tables
--   3. Widen journey_location_type from VARCHAR(2) to VARCHAR(20)

-- ============================================================
-- 1. Rename PM audit tables: _aud -> _audit
-- ============================================================
ALTER TABLE pm_timetable_years_aud RENAME TO pm_timetable_years_audit;
ALTER TABLE pm_reference_trains_aud RENAME TO pm_reference_trains_audit;
ALTER TABLE pm_routes_aud RENAME TO pm_routes_audit;
ALTER TABLE pm_path_requests_aud RENAME TO pm_path_requests_audit;
ALTER TABLE pm_paths_aud RENAME TO pm_paths_audit;
ALTER TABLE pm_train_versions_aud RENAME TO pm_train_versions_audit;
ALTER TABLE pm_journey_locations_aud RENAME TO pm_journey_locations_audit;

-- ============================================================
-- 2. Rename Envers columns: rev -> revision_id, revtype -> revision_type
--    (V10 renamed them to rev/revtype when Envers defaults were in effect)
-- ============================================================
ALTER TABLE pm_timetable_years_audit RENAME COLUMN rev TO revision_id;
ALTER TABLE pm_timetable_years_audit RENAME COLUMN revtype TO revision_type;

ALTER TABLE pm_reference_trains_audit RENAME COLUMN rev TO revision_id;
ALTER TABLE pm_reference_trains_audit RENAME COLUMN revtype TO revision_type;

ALTER TABLE pm_routes_audit RENAME COLUMN rev TO revision_id;
ALTER TABLE pm_routes_audit RENAME COLUMN revtype TO revision_type;

ALTER TABLE pm_path_requests_audit RENAME COLUMN rev TO revision_id;
ALTER TABLE pm_path_requests_audit RENAME COLUMN revtype TO revision_type;

ALTER TABLE pm_paths_audit RENAME COLUMN rev TO revision_id;
ALTER TABLE pm_paths_audit RENAME COLUMN revtype TO revision_type;

ALTER TABLE pm_train_versions_audit RENAME COLUMN rev TO revision_id;
ALTER TABLE pm_train_versions_audit RENAME COLUMN revtype TO revision_type;

ALTER TABLE pm_journey_locations_audit RENAME COLUMN rev TO revision_id;
ALTER TABLE pm_journey_locations_audit RENAME COLUMN revtype TO revision_type;

-- ============================================================
-- 3. Widen journey_location_type from VARCHAR(2) to VARCHAR(20)
--    The column stores TTT JourneyLocationTypeCode which is nominally a 2-digit
--    code ("01", "02", etc.) but the API may pass enum names like "INTERMEDIATE".
-- ============================================================
ALTER TABLE pm_journey_locations ALTER COLUMN journey_location_type TYPE VARCHAR(20);
ALTER TABLE pm_journey_locations_audit ALTER COLUMN journey_location_type TYPE VARCHAR(20);
