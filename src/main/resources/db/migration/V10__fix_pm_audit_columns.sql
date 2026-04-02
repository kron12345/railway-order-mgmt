-- Fix Envers audit column names: revision_id->rev, revision_type->revtype
-- Envers expects 'rev' and 'revtype' by default

ALTER TABLE pm_timetable_years_aud RENAME COLUMN revision_id TO rev;
ALTER TABLE pm_timetable_years_aud RENAME COLUMN revision_type TO revtype;

ALTER TABLE pm_reference_trains_aud RENAME COLUMN revision_id TO rev;
ALTER TABLE pm_reference_trains_aud RENAME COLUMN revision_type TO revtype;

ALTER TABLE pm_routes_aud RENAME COLUMN revision_id TO rev;
ALTER TABLE pm_routes_aud RENAME COLUMN revision_type TO revtype;

ALTER TABLE pm_path_requests_aud RENAME COLUMN revision_id TO rev;
ALTER TABLE pm_path_requests_aud RENAME COLUMN revision_type TO revtype;

ALTER TABLE pm_paths_aud RENAME COLUMN revision_id TO rev;
ALTER TABLE pm_paths_aud RENAME COLUMN revision_type TO revtype;

ALTER TABLE pm_train_versions_aud RENAME COLUMN revision_id TO rev;
ALTER TABLE pm_train_versions_aud RENAME COLUMN revision_type TO revtype;

ALTER TABLE pm_journey_locations_aud RENAME COLUMN revision_id TO rev;
ALTER TABLE pm_journey_locations_aud RENAME COLUMN revision_type TO revtype;
