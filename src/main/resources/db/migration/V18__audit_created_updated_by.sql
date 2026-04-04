-- Add createdBy/updatedBy to all audited entities that lack them
ALTER TABLE order_positions ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE order_positions ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE order_positions_audit ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE order_positions_audit ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE resource_needs ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE resource_needs ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE resource_needs_audit ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE resource_needs_audit ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE purchase_positions ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE purchase_positions ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE purchase_positions_audit ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE purchase_positions_audit ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE resource_catalog_items ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE resource_catalog_items ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE resource_catalog_items_audit ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE resource_catalog_items_audit ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE timetable_archives ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE timetable_archives ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE timetable_archives_audit ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE timetable_archives_audit ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

-- PM entities (these correctly use _aud suffix from V9)
ALTER TABLE pm_reference_trains ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_reference_trains ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE pm_reference_trains_aud ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_reference_trains_aud ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pm_train_versions ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_train_versions ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE pm_train_versions_aud ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_train_versions_aud ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pm_path_requests ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_path_requests ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE pm_path_requests_aud ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_path_requests_aud ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pm_paths ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_paths ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE pm_paths_aud ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_paths_aud ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pm_routes ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_routes ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE pm_routes_aud ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_routes_aud ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pm_timetable_years ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_timetable_years ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE pm_timetable_years_aud ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_timetable_years_aud ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pm_journey_locations ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_journey_locations ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE pm_journey_locations_aud ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pm_journey_locations_aud ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
