-- V22: Add missing columns to resource_catalog_items_audit
--
-- V16 created the audit table without created_at, updated_at, and version.

ALTER TABLE resource_catalog_items_audit
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;

ALTER TABLE resource_catalog_items_audit
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

ALTER TABLE resource_catalog_items_audit
    ADD COLUMN IF NOT EXISTS version BIGINT;
